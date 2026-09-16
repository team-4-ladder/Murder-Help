import type { ChatMessageResponse, BotMessageDto } from "./chat.types";
import { TIERS, gradeToTier } from "../../lib/tier";
import { ORDER_STATUS_LABEL } from "../../lib/order";

interface ChatMessageBubbleProps {
  message: ChatMessageResponse;
  customerId: number;
  isAdmin: boolean;
  isCompleted: boolean;
  isLatest?: boolean;
  onSendBotOption: (label: string) => void;
}

export function ChatMessageBubble({ message: m, customerId, isAdmin, isCompleted, isLatest = false, onSendBotOption }: ChatMessageBubbleProps) {
  const isBot = m.senderEmail === "bot@system.com";
  
  // 여기서 customerId는 항상 '현재 로그인한 사용자'의 ID입니다. (고객이든 관리자든 상관없이)
  const isMe = m.memberId === customerId;
  const isOtherAdmin = !isMe && !isBot && !isAdmin;
  const isOtherCustomer = !isMe && !isBot && isAdmin;

  let botData: BotMessageDto | null = null;
  let botParseFailed = false;
  if (m.messageType === "BUTTON") {
    try {
      botData = JSON.parse(m.content) as BotMessageDto;
    } catch (e) {
      console.error("BotMessageDto 파싱 오류 (JSON 형식 확인 필요):", e, "원본 내용:", m.content);
      botParseFailed = true;
    }
  }

  const isSenderAdmin = isAdmin ? isMe : isOtherAdmin;
  const isSenderCustomer = isAdmin ? isOtherCustomer : isMe;

  let bgColor = "rgba(255,255,255,0.05)";
  let borderColor = "transparent";
  let textColor = "#e2e8f0";

  if (isSenderAdmin) {
    bgColor = "#064e3b"; // emerald-900
    textColor = "#ecfdf5";
  } else if (isSenderCustomer) {
    bgColor = "#27272a"; // zinc-800
    textColor = "#f4f4f5";
  }

  const handleOptionClick = (label: string) => {
    if (isAdmin) {
      alert("관리자는 챗봇을 조작할 수 없습니다.");
      return;
    }
    if (!isCompleted) {
      onSendBotOption(label);
    }
  };

  // 시스템 메시지 또는 봇 텍스트 메시지 (버튼 없는 경우)
  if (m.messageType === "SYSTEM" || (isBot && !botData)) {
    return (
      <div className="flex justify-center my-4 w-full px-4">
        <div className="bg-neutral-900/90 border border-zinc-700/50 rounded-full px-4 py-1.5 flex items-center gap-2 w-fit shadow-sm">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="w-3.5 h-3.5 text-zinc-400 shrink-0">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M12 16v-4"></path>
            <path d="M12 8h.01"></path>
          </svg>
          <span className="text-xs text-zinc-300 tracking-wide text-center break-keep pt-[1px]">
            {botParseFailed ? "메시지를 표시할 수 없습니다." : m.content}
          </span>
        </div>
      </div>
    );
  }

  // 봇 버튼 메시지
  if (isBot && botData) {
    return (
      <div className="flex justify-center my-5 w-full px-2">
        <div className="bg-neutral-900/95 border border-zinc-700/50 rounded-2xl px-3 py-4 w-full max-w-[400px] shadow-lg flex flex-col items-center">
          <div className="flex items-center gap-1.5 mb-3 w-full justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0 w-4 h-4 aspect-square text-emerald-400">
              <rect x="3" y="11" width="18" height="10" rx="2"></rect>
              <circle cx="12" cy="5" r="2"></circle>
              <path d="M12 7v4"></path>
              <line x1="8" y1="16" x2="8.01" y2="16"></line>
              <line x1="16" y1="16" x2="16.01" y2="16"></line>
            </svg>
            <span className="text-xs text-emerald-400 font-semibold tracking-widest font-mono pt-[1px]">SYSTEM MENU</span>
          </div>
          <div
            className={`text-[12.5px] tracking-tight text-zinc-300 leading-relaxed w-full px-2 mb-4 ${
              botData.title?.includes('최근 주문 내역 안내') && botData.orderId != null
                ? (isCompleted ? 'opacity-70 cursor-not-allowed' : 'cursor-pointer hover:bg-zinc-800/40 p-2 rounded-lg transition-colors border border-transparent hover:border-zinc-700/50')
                : ''
            }`}
            onClick={() => {
              if (botData?.title?.includes('최근 주문 내역 안내') && botData.orderId != null) {
                if (isCompleted) return;
                const view = { name: "mypage", section: "orders", orderId: botData.orderId };
                window.history.pushState({ view }, "");
                window.dispatchEvent(new PopStateEvent("popstate", { state: { view } }));
              }
            }}
          >
            {/* 가운데 정렬될 제목 */}
            {botData.title && (
              <div className="text-center mb-1 font-semibold whitespace-pre-wrap">
                {botData.title}
              </div>
            )}
            {/* 왼쪽 정렬될 본문 */}
            {(botData.text || botData.orderStatus) && (
              <div className="text-left break-keep whitespace-pre-wrap">
                {botData.text}
                {botData.orderStatus && (
                  <>
                    {botData.text && "\n"}
                    {`▪️ 주문 상태: ${ORDER_STATUS_LABEL[botData.orderStatus as keyof typeof ORDER_STATUS_LABEL] ?? botData.orderStatus}`}
                  </>
                )}
              </div>
            )}
          </div>
          
          {botData.products && botData.products.length > 0 && (
            <div className="w-full flex flex-col gap-2 mb-3">
              {botData.products.map((p, pIdx) => (
                <div 
                  key={pIdx} 
                  onClick={() => {
                    if (isCompleted) return;
                    window.history.pushState({ view: { name: "detail", id: p.id } }, "");
                    window.dispatchEvent(new PopStateEvent("popstate", { state: { view: { name: "detail", id: p.id } } }));
                  }}
                  className={`text-xs p-2.5 rounded-lg flex justify-between bg-zinc-800/40 border border-zinc-700/50 transition-colors ${
                    isCompleted ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer hover:bg-zinc-700/60'
                  }`}
                >
                  <span className="text-zinc-200">{p.name}</span>
                  <span className="text-emerald-400 font-mono">₩{p.price.toLocaleString()}</span>
                </div>
              ))}
            </div>
          )}
          
          {botData.options && botData.options.length > 0 && (
            <div className="w-full flex flex-col gap-2">
              {botData.options.map((opt, oIdx) => {
                const isOptionDisabled = isAdmin || !isLatest;
                return (
                  <button
                    key={oIdx}
                    disabled={isOptionDisabled}
                    onClick={() => handleOptionClick(opt.label)}
                    className={`text-[13px] font-medium py-2 px-4 rounded-xl border text-center transition-all ${isOptionDisabled ? "opacity-40 cursor-not-allowed border-zinc-800 text-zinc-500 bg-zinc-900/40" : "hover:bg-emerald-900/30 hover:text-emerald-300 border-emerald-900/50 text-emerald-400/90 bg-emerald-950/20"}`}
                  >
                    {opt.label}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>
    );
  }

  // 일반 채팅 메시지 (관리자/고객)
  if (isMe) {
    return (
      <div className="flex flex-col mb-5 items-end">
        <div className="flex items-end gap-1.5 max-w-full flex-row-reverse">
          {/* 말풍선 본체 */}
          <div
            className="max-w-[85%] break-words px-3.5 py-2 text-[13px] leading-relaxed tracking-wide shadow-sm rounded-2xl rounded-tr-sm"
            style={{
              background: bgColor,
              color: textColor,
              border: borderColor
            }}
          >
            <div className="whitespace-pre-wrap leading-relaxed">{m.content}</div>
          </div>

          {/* 말풍선 바깥 시간 (카카오톡 스타일) */}
          <div className="text-[10px] text-zinc-500 shrink-0 mb-1 font-mono tracking-tighter">
            {m.createdAt.slice(11, 16)}
          </div>
        </div>
      </div>
    );
  }

  // 상대방 메시지: 좌측 아바타 + 우측 콘텐츠 2컬럼 구조
  return (
    <div className="flex items-start gap-2.5 mb-5">
      {/* 좌측 컬럼: 프로필 아바타 */}
      <div
        className="w-6 h-6 rounded-full overflow-hidden border bg-[#240606] shrink-0 flex items-center justify-center"
        style={{ borderColor: m.senderGrade ? TIERS[gradeToTier(m.senderGrade)].brightColor : "#3f3f46" }}
      >
        {m.senderProfileImageUrl ? (
          <img src={m.senderProfileImageUrl} alt="profile" className="w-full h-full object-cover" />
        ) : (
          <svg viewBox="0 0 100 100" width="16" height="16" aria-label="기본 프로필 이미지">
            <circle cx="50" cy="34" r="17" fill="#8b544d" />
            <path d="M20 88c4-21 17-31 30-31s26 10 30 31" fill="#8b544d" />
          </svg>
        )}
      </div>

      {/* 우측 컬럼: 이름/뱃지 헤더 + 말풍선 */}
      <div className="flex flex-col gap-1 min-w-0">
        <div className="flex items-center gap-1.5 text-[11px]" style={{ fontFamily: "Noto Sans KR, sans-serif" }}>
          <span className="font-semibold tracking-wide text-zinc-300">{m.senderName}</span>
        </div>

        <div className="flex items-end gap-1.5 max-w-full">
          {/* 말풍선 본체 */}
          <div
            className="max-w-[85%] break-words px-3.5 py-2 text-[13px] leading-relaxed tracking-wide shadow-sm rounded-2xl rounded-tl-sm"
            style={{
              background: bgColor,
              color: textColor,
              border: borderColor
            }}
          >
            <div className="whitespace-pre-wrap leading-relaxed">{m.content}</div>
          </div>

          {/* 말풍선 바깥 시간 (카카오톡 스타일) */}
          <div className="text-[10px] text-zinc-500 shrink-0 mb-1 font-mono tracking-tighter">
            {m.createdAt.slice(11, 16)}
          </div>
        </div>
      </div>
    </div>
  );
}
