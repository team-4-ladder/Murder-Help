import { useEffect, useRef, useState } from "react";
import ChatRoomView from "./ChatRoomView";
import type {ChatRoomResponse, PageResponse} from "./chat.types";
import { getAccessToken } from "@/api/client.ts";
import { TierBadge } from "../member/TierBadge";
import { TIERS, gradeToTier } from "../../lib/tier";
import { subscribeChatSocket } from "./chatSocket";

type TabType = "ALL" | "WAITING" | "IN_PROGRESS" | "COMPLETED";

export default function AdminChatDashboard({ adminId }: { adminId: number }) {
  const [rooms, setRooms] = useState<ChatRoomResponse[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>("ALL");
  const [searchInput, setSearchInput] = useState("");
  const [searchKeyword, setSearchKeyword] = useState("");
  const activeTabRef = useRef<TabType>(activeTab);
  const selectedRoomIdRef = useRef<number | null>(selectedRoomId);
  const fetchRoomsRequestId = useRef(0);
  const fetchTabCountsRequestId = useRef(0);

  const [isError, setIsError] = useState(false);
  const [waitingCount, setWaitingCount] = useState(0);
  const [inProgressCount, setInProgressCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  // 현재 탭 상태와 선택된 방 상태를 Ref에 동기화 (웹소켓 클로저에서 최신 상태 참조)
  useEffect(() => {
    activeTabRef.current = activeTab;
  }, [activeTab]);

  useEffect(() => {
    selectedRoomIdRef.current = selectedRoomId;
  }, [selectedRoomId]);

  // 🔹 별도로 WAITING과 IN_PROGRESS 개수를 병렬로 가져오는 함수 (size=1)
  const fetchTabCounts = async () => {
    const requestId = ++fetchTabCountsRequestId.current;
    try {
      const headers = { Authorization: `Bearer ${getAccessToken()}` };
      const [waitingRes, inProgressRes] = await Promise.all([
        fetch(`/api/chat/rooms?status=WAITING&size=1`, { headers }),
        fetch(`/api/chat/rooms?status=IN_PROGRESS&size=1`, { headers })
      ]);

      // 더 최근 호출이 이미 시작됐다면 이 응답은 오래된 것이므로 무시한다.
      if (requestId !== fetchTabCountsRequestId.current) return;

      if (waitingRes.ok) {
        const json = await waitingRes.json();
        const pageData = (json.data ? json.data : json) as PageResponse<ChatRoomResponse>;
        setWaitingCount(pageData.totalElements || 0);
      }

      if (inProgressRes.ok) {
        const json = await inProgressRes.json();
        const pageData = (json.data ? json.data : json) as PageResponse<ChatRoomResponse>;
        setInProgressCount(pageData.totalElements || 0);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const fetchRooms = async () => {
    const requestId = ++fetchRoomsRequestId.current;
    setIsLoading(true);
    setIsError(false);
    try {
      const query = new URLSearchParams();
      if (activeTab !== "ALL") query.append("status", activeTab);
      if (searchKeyword.trim() !== "") query.append("keyword", searchKeyword);

      const res = await fetch(`/api/chat/rooms?${query.toString()}&size=100`, {
        headers: { Authorization: `Bearer ${getAccessToken()}` }
      });
      if (!res.ok) throw new Error("방 목록 조회 실패");
      const json = await res.json();
      const pageData = (json.data ? json.data : json) as PageResponse<ChatRoomResponse>;

      // 더 최근 탭 전환/검색으로 인한 요청이 이미 시작됐다면 이 응답은 버린다.
      if (requestId !== fetchRoomsRequestId.current) return;

      setRooms(pageData.content);
      fetchTabCounts();
    } catch (err) {
      if (requestId !== fetchRoomsRequestId.current) return;
      console.warn("방 목록 조회 에러:", err);
      setIsError(true);
    } finally {
      if (requestId === fetchRoomsRequestId.current) setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchRooms();
  }, [activeTab, searchKeyword]);

  useEffect(() => {
    const unsubscribe = subscribeChatSocket("/sub/chat/rooms/updates", (body) => {
      const updatedRoom = JSON.parse(body) as ChatRoomResponse;

      setRooms(prev => {
        const currentTab = activeTabRef.current;
        const currentRoomId = selectedRoomIdRef.current;

        // ✨ 핵심: 내가 현재 열어둔 방의 상태가 변했고, 그 상태가 현재 탭과 다를 때
        if (currentRoomId === updatedRoom.roomId && currentTab !== "ALL" && updatedRoom.status !== currentTab) {
          // 탭을 새로운 상태로 강제 전환 (이후 fetchRooms가 돌아가서 목록이 갱신됨)
          setActiveTab(updatedRoom.status as TabType);
          return prev; // 탭이 바뀌면서 리스트 전체를 다시 불러오므로 여기선 가만히 둠
        }

        // 내가 열어둔 방이 아니라면 기존처럼 조용히 목록 갱신
        if (currentTab !== "ALL" && updatedRoom.status !== currentTab) {
          return prev.filter(r => r.roomId !== updatedRoom.roomId);
        }

        const exists = prev.find(r => r.roomId === updatedRoom.roomId);
        if (exists) {
          return prev.map(r => r.roomId === updatedRoom.roomId ? updatedRoom : r);
        } else {
          return [updatedRoom, ...prev];
        }
      });

      // WebSocket으로 상태 변경 알림이 오면 묻지도 따지지도 않고 전체 카운트 단건 갱신
      fetchTabCounts();
    });

    return () => {
      unsubscribe();
    };
  }, []);

  // 현재 선택된 방이 (탭 전환/검색/실시간 갱신 등으로) 목록에서 사라지면
  // 목록의 첫 방으로 자동 전환하거나, 목록이 비었으면 선택을 완전히 해제한다.
  useEffect(() => {
    const stillExists = rooms.some(r => r.roomId === selectedRoomId);
    if (stillExists) return;
    setSelectedRoomId(rooms.length > 0 ? rooms[0].roomId : null);
  }, [rooms, selectedRoomId]);

  const selectedRoom = rooms.find(r => r.roomId === selectedRoomId);
  const isSelectedRoomCompleted = selectedRoom?.status === 'COMPLETED';

  return (
    // 전체 배경 컨테이너 (부드러운 차콜 다크: Zinc-950)
    <div className="flex flex-col h-[85vh] m-6 bg-[#0e0e11] border border-[#27272a] rounded-2xl overflow-hidden text-[#f4f4f5] shadow-[0_8px_30px_rgba(0,0,0,0.5)] font-sans tracking-tight">
      
      {/* 상단 통합 헤더 (Top Header) - Zinc-900 */}
      <div className="flex justify-between items-center px-8 py-5 bg-[#18181b] border-b border-[#27272a]">
        <div className="flex items-center gap-3">
          <h2 className="text-xl font-semibold text-[#f4f4f5] tracking-tight">Chat Management</h2>
        </div>
        <div className="flex items-center gap-6 text-sm text-[#a1a1aa]">
           <span>Admin ID: {adminId}</span>
           <div className="flex items-center gap-2 px-3 py-1 bg-[#27272a] border border-[#3f3f46] rounded-full shadow-sm">
             <div className="w-1.5 h-1.5 rounded-full bg-emerald-400"></div>
             <span className="text-[#e4e4e7] font-medium text-xs">System Online</span>
           </div>
        </div>
      </div>

      {/* 메인 콘텐츠 영역 */}
      <div className="flex flex-col lg:flex-row flex-1 gap-4 lg:gap-6 p-4 lg:p-6 bg-[#0e0e11] overflow-hidden">
        
        {/* 왼쪽 (상단) 카드: 채팅방 리스트 */}
        <div className="min-w-[280px] h-1/2 lg:h-auto lg:w-1/3 shrink-0 flex flex-col bg-[#18181b] border border-[#27272a] rounded-xl shadow-lg overflow-hidden">
          
          {/* 탭 영역 */}
          <div className="p-3 lg:p-4 border-b border-[#27272a]">
            <div 
              className="flex overflow-x-auto bg-[#09090b] rounded-lg p-1 border border-[#27272a]/50" 
              style={{ msOverflowStyle: 'none', scrollbarWidth: 'none' }}
            >
              {(["ALL", "WAITING", "IN_PROGRESS", "COMPLETED"] as TabType[]).map(tab => {
                const isActive = activeTab === tab;
                return (
                  <button 
                    key={tab} 
                    onClick={() => setActiveTab(tab)}
                    className={`flex-1 shrink-0 whitespace-nowrap py-1.5 px-3 text-xs font-medium text-center rounded-md transition-all duration-200 ${
                      isActive ? 'bg-[#3f3f46] text-[#f4f4f5] shadow-sm' : 'text-[#a1a1aa] hover:text-[#d4d4d8]'
                    }`}
                  >
                    <span className="relative inline-block pr-1">
                      {tab.replace('_', ' ')}
                      {tab === "WAITING" && waitingCount > 0 && (
                        <span className={`absolute -top-1.5 -right-2 px-1 py-0 rounded-full text-[8px] font-bold shadow-sm ${
                          isActive ? "bg-[#f4f4f5] text-[#09090b]" : "bg-[#3f3f46] text-[#d4d4d8]"
                        }`} style={{ lineHeight: '1.2' }}>
                          {waitingCount}
                        </span>
                      )}
                      {tab === "IN_PROGRESS" && inProgressCount > 0 && (
                        <span className={`absolute -top-1.5 -right-2 px-1 py-0 rounded-full text-[8px] font-bold shadow-sm ${
                          isActive ? "bg-[#f4f4f5] text-[#09090b]" : "bg-[#3f3f46] text-[#d4d4d8]"
                        }`} style={{ lineHeight: '1.2' }}>
                          {inProgressCount}
                        </span>
                      )}
                    </span>
                  </button>
                )
              })}
            </div>
          </div>

          {/* 검색바 영역 */}
          <div className="p-4 border-b border-[#27272a]">
            <div className="relative">
              <input 
                type="text" 
                placeholder="이름, 이메일, 방 제목 검색 (Enter)..." 
                value={searchInput}
                onChange={e => setSearchInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && setSearchKeyword(searchInput)}
                className="w-full bg-[#0e0e11] text-[#f4f4f5] text-sm py-2.5 px-9 rounded-lg border border-[#27272a] focus:border-[#52525b] focus:outline-none transition-all placeholder-[#71717a] shadow-inner"
              />
              <div className="absolute left-3 top-3 text-[#71717a]">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
              </div>
            </div>
          </div>

          {/* 리스트 영역 */}
          <div className="flex-1 overflow-y-scroll custom-scrollbar p-3 space-y-1">
            {isError && (
              <div className="p-4 text-center text-red-400 text-sm bg-red-950/20 rounded-lg border border-red-900/30">
                Failed to load chat history. System offline.
              </div>
            )}
            {rooms.map(room => {
              const isSelected = selectedRoomId === room.roomId;
              
              let badgeStyle = 'bg-[#27272a] text-[#a1a1aa] border-[#3f3f46]';
              if (room.status === 'BOT_MODE') badgeStyle = 'bg-violet-500/10 text-violet-300 border-violet-500/30';
              else if (room.status === 'WAITING') badgeStyle = 'bg-[#f4f4f5] text-[#18181b] border-[#f4f4f5] shadow-sm';
              else if (room.status === 'COMPLETED') badgeStyle = 'bg-transparent text-[#71717a] border-[#27272a]';

              return (
                <div 
                  key={room.roomId}
                  onClick={() => setSelectedRoomId(room.roomId)}
                  className={`p-3 rounded-lg cursor-pointer transition-all duration-150 border flex gap-3 items-center ${
                    isSelected 
                      ? 'bg-[#27272a] border-[#3f3f46] shadow-sm' 
                      : 'bg-transparent border-transparent hover:bg-[#27272a]/50'
                  }`}
                >
                  {/* 프로필 이미지 (Avatar) */}
                  <div
                    className="w-10 h-10 flex-shrink-0 rounded-full bg-[#240606] border-2 overflow-hidden flex items-center justify-center shadow-inner"
                    style={{ borderColor: room.customerGrade ? TIERS[gradeToTier(room.customerGrade)].brightColor : "#3f3f46" }}
                  >
                    {room.customerProfileImageUrl ? (
                      <img src={room.customerProfileImageUrl} alt="profile" className="w-full h-full object-cover" />
                    ) : (
                      <svg viewBox="0 0 100 100" width="24" height="24" aria-label="기본 프로필 이미지">
                        <circle cx="50" cy="34" r="17" fill="#8b544d" />
                        <path d="M20 88c4-21 17-31 30-31s26 10 30 31" fill="#8b544d" />
                      </svg>
                    )}
                  </div>
                  
                  {/* 메시지 정보 영역 */}
                  <div className="flex-1 min-w-0 space-y-0.5">
                    <div className="flex justify-between items-center">
                      <span className={`font-semibold text-sm truncate pr-2 ${isSelected ? 'text-[#f4f4f5]' : 'text-[#d4d4d8]'}`}>
                        {room.title}
                      </span>
                      <span className={`text-[9px] px-1.5 py-0.5 rounded border font-medium whitespace-nowrap flex-shrink-0 ${badgeStyle}`}>
                        {room.status.replace('_', ' ')}
                      </span>
                    </div>
                    <span className="block text-xs text-neutral-400 truncate">
                      {room.customerName ?? "—"} · {room.customerEmail ?? "—"}
                    </span>
                    <div className="flex items-center justify-between gap-2">
                      <span className={`text-xs truncate min-w-0 ${isSelected ? 'text-[#a1a1aa]' : 'text-[#71717a]'}`}>
                        {room.status === "COMPLETED"
                          ? "상담이 종료된 방입니다."
                          : (room.lastMessage ?? "새로운 대화가 없습니다.")}
                      </span>
                      <span className="shrink-0 text-[11px] text-neutral-500">
                        {room.updatedAt.slice(11, 16)}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
            {rooms.length === 0 && !isError && (
              <div className="p-8 mt-10 flex flex-col items-center justify-center text-[#71717a] text-sm">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="mb-3"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                No previous chats found.
              </div>
            )}
          </div>
        </div>

        {/* 오른쪽 카드: 채팅 화면 */}
        <div className="flex-1 flex flex-col bg-[#18181b] border border-[#27272a] rounded-xl shadow-lg overflow-hidden">
          {selectedRoom ? (
            <>
              <div className="px-8 py-5 border-b border-[#27272a] flex justify-between items-center bg-[#18181b] z-10">
                <div className="flex flex-col gap-0.5 min-w-0">
                  <h3 className={`font-semibold text-lg tracking-tight ${isSelectedRoomCompleted ? 'text-[#71717a]' : 'text-[#f4f4f5]'}`}>
                    Support Channel #{selectedRoom.roomId}
                  </h3>
                  <div className="flex items-center gap-1.5 text-xs text-neutral-400 min-w-0">
                    <span className="truncate min-w-0 flex-1">
                      {selectedRoom.customerName ?? "—"} · {selectedRoom.customerEmail ?? "—"}
                    </span>
                    {selectedRoom.customerGrade && (
                      <TierBadge tier={gradeToTier(selectedRoom.customerGrade)} compact />
                    )}
                  </div>
                </div>
                {!isSelectedRoomCompleted && (
                  <button
                    onClick={() => {
                      fetch(`/api/chat/rooms/${selectedRoom.roomId}/close`, {
                        method: "PATCH",
                        headers: {
                          Authorization: `Bearer ${getAccessToken()}`
                        }
                      })
                        .then(res => {
                          if (!res.ok) throw new Error("채널 닫기 실패");
                        })
                        .catch(err => alert(err.message));
                    }}
                    className="px-4 py-1.5 bg-[#27272a] hover:bg-[#3f3f46] text-[#e4e4e7] border border-[#3f3f46] rounded-md text-xs font-medium transition-colors shadow-sm"
                  >
                    Close Channel
                  </button>
                )}
              </div>
              <div className="flex-1 relative overflow-hidden bg-[#0e0e11]">
                <ChatRoomView key={selectedRoom.roomId} roomId={selectedRoom.roomId} customerId={adminId} isAdmin={true} />
              </div>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-[#52525b]">
              <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" className="mb-4">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
              <p className="text-sm font-medium">대화할 채팅방을 선택해 주세요</p>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
