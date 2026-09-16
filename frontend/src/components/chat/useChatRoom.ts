import { useEffect, useRef, useState, useLayoutEffect } from "react";
import type { ChatMessageResponse, ChatRoomResponse, ChatRoomStatus } from "./chat.types";
import { getAccessToken } from "../../api/client";
import { subscribeChatSocket, onChatSocketReconnect, isChatSocketConnected, publishChatSocket } from "./chatSocket";

export function useChatRoom(roomId: number) {
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [lastMessageId, setLastMessageId] = useState<number | null>(null);
  const [hasNext, setHasNext] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false);
  const [status, setStatus] = useState<ChatRoomStatus>("BOT_MODE");
  const [isError, setIsError] = useState(false);
  const [showScrollBottom, setShowScrollBottom] = useState(false);

  const containerRef = useRef<HTMLDivElement>(null);
  const previousScrollHeight = useRef<number>(0);
  const isFetchingHistory = useRef(false);
  const isInitialMount = useRef(true);
  const pendingScrollTimeouts = useRef<Set<ReturnType<typeof setTimeout>>>(new Set());

  // 1. 초기 데이터 및 상태 조회, STOMP 구독
  useEffect(() => {
    fetch(`/api/chat/rooms/${roomId}`, {
      headers: { Authorization: `Bearer ${getAccessToken()}` }
    })
      .then(res => res.json())
      .then(json => {
        if (json.data) {
          setStatus(json.data.status);
          if (json.data.status === "COMPLETED") setIsCompleted(true);
        }
      })
      .catch(err => console.warn("방 상태 조회 실패:", err));

    loadMoreMessages(true);

    const unsubscribeMessages = subscribeChatSocket(`/sub/chat/room/${roomId}`, (body) => {
      const newMsg = JSON.parse(body) as ChatMessageResponse;
      setMessages(prev => prev.some(m => m.id === newMsg.id) ? prev : [...prev, newMsg]);

      if (newMsg.messageType === "SYSTEM" && newMsg.content.includes("[CLOSED]")) {
        setIsCompleted(true);
      }

      const timeoutId = setTimeout(() => {
         pendingScrollTimeouts.current.delete(timeoutId);
         if (containerRef.current) {
            containerRef.current.scrollTo({
              top: containerRef.current.scrollHeight,
              behavior: "auto"
            });
         }
      }, 50);
      pendingScrollTimeouts.current.add(timeoutId);
    });

    // 실시간 방 상태 업데이트 구독 (상담사 연결 등으로 상태가 변경될 때 즉각 반영)
    const unsubscribeRoomUpdates = subscribeChatSocket('/sub/chat/rooms/updates', (body) => {
      try {
        const updatedRoom = JSON.parse(body) as ChatRoomResponse;
        if (updatedRoom.roomId === roomId) {
          setStatus(updatedRoom.status);
          if (updatedRoom.status === "COMPLETED") {
            setIsCompleted(true);
          }
        }
      } catch (e) {
        console.error(e);
      }
    });

    // 재연결된 경우, 끊겨 있던 동안 놓쳤을 수 있는 메시지를 최신 페이지로 다시 채운다.
    const unsubscribeReconnect = onChatSocketReconnect(() => {
      isInitialMount.current = true;
      loadMoreMessages(true);
    });

    return () => {
      unsubscribeMessages();
      unsubscribeRoomUpdates();
      unsubscribeReconnect();
      pendingScrollTimeouts.current.forEach(id => clearTimeout(id));
      pendingScrollTimeouts.current.clear();
    };
  }, [roomId]);

  // 2. 과거 메시지 로딩 함수 (lastMessageId 커서 기반)
  const loadMoreMessages = async (isInitial = false) => {
    if (isFetchingHistory.current) return;
    isFetchingHistory.current = true;
    setIsLoading(true);
    setIsError(false);

    try {
      const cursorParam = !isInitial && lastMessageId != null ? `&lastMessageId=${lastMessageId}` : "";
      const res = await fetch(`/api/chat/rooms/${roomId}/messages?size=20${cursorParam}`, {
        headers: { Authorization: `Bearer ${getAccessToken()}` }
      });
      if (!res.ok) throw new Error("메시지 내역 조회 실패");
      const json = await res.json();

      if (json.data && json.data.content) {
        const newMsgs = [...json.data.content].reverse();

        if (!isInitial && containerRef.current) {
          previousScrollHeight.current = containerRef.current.scrollHeight;
        }

        setMessages(prev => isInitial ? newMsgs : [...newMsgs, ...prev]);
        setHasNext(json.data.hasNext);
        setLastMessageId(json.data.nextCursorId);
      }
    } catch (error) {
      console.warn("과거 메시지 로드 에러:", error);
      setIsError(true);
    } finally {
      setIsLoading(false);
      isFetchingHistory.current = false;
    }
  };

  // 3. 스크롤 위치 보정
  useLayoutEffect(() => {
    if (containerRef.current) {
      if (isInitialMount.current && messages.length > 0) {
        containerRef.current.scrollTop = containerRef.current.scrollHeight;
        isInitialMount.current = false;
      } else if (previousScrollHeight.current > 0) {
        const currentScrollHeight = containerRef.current.scrollHeight;
        containerRef.current.scrollTop = currentScrollHeight - previousScrollHeight.current;
        previousScrollHeight.current = 0;
      }
    }
  }, [messages]);

  // 4. 스크롤 이벤트 핸들러
  const handleScroll = () => {
    if (containerRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = containerRef.current;
      setShowScrollBottom(scrollHeight - scrollTop - clientHeight > 100);

      if (scrollTop === 0 && hasNext && !isLoading) {
        loadMoreMessages(false);
      }
    }
  };

  const scrollToBottom = () => {
    if (containerRef.current) {
      containerRef.current.scrollTo({
        top: containerRef.current.scrollHeight,
        behavior: "smooth"
      });
    }
  };

  const sendMessage = (customerId: number, content: string) => {
    if (!content.trim() || isCompleted || !isChatSocketConnected()) return;
    publishChatSocket("/pub/chat.send", JSON.stringify({ roomId, memberId: customerId, content }));
  };

  return {
    messages,
    isLoading,
    isCompleted,
    status,
    isError,
    showScrollBottom,
    containerRef,
    handleScroll,
    scrollToBottom,
    sendMessage
  };
}
