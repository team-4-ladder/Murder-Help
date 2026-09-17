import { useState } from "react";
import { useChatRoom } from "./useChatRoom";
import { ChatMessageBubble } from "./ChatMessageBubble";
import { getAccessToken } from "../../api/client";

export default function ChatRoomView({ roomId, customerId, isAdmin = false }: { roomId: number; customerId: number; isAdmin?: boolean }) {
  const [input, setInput] = useState("");
  
  const {
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
  } = useChatRoom(roomId);

  const isAdminBotMode = isAdmin && status === "BOT_MODE";
  const isInputDisabled = isCompleted || isAdminBotMode;

  const send = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isInputDisabled) return;
    sendMessage(customerId, input);
    setInput("");
  };

  return (
    <div className="flex flex-col h-full bg-chat-black relative">
      {/* 상단 고정 배너 (고객 화면 & 진행 중일 때만) */}
      {!isAdmin && !isCompleted && (
        <div className="shrink-0 flex justify-between items-center px-4 py-2.5 bg-chat-primary/10 border-b border-chat-primary/50 z-10">
          <span className="text-[10px] text-[#ff4422] font-mono tracking-widest flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-[#ff4422] animate-pulse"></span>
            SYSTEM ONLINE
          </span>
          <button
            onClick={() => {
              if (window.confirm("현재 통신을 완전히 종료하시겠습니까?")) {
                fetch(`/api/chat/rooms/${roomId}/close`, {
                  method: "PATCH",
                  headers: {
                    Authorization: `Bearer ${getAccessToken()}`
                  }
                })
                  .then(res => {
                    if (!res.ok) throw new Error("채널 닫기 실패");
                  })
                  .catch(err => alert(err.message));
              }
            }}
            className="text-[10px] font-bold px-3 py-1 rounded transition-colors uppercase tracking-widest border border-chat-primary text-[#ff4422] hover:bg-chat-primary hover:text-white bg-black/50 font-mono"
          >
            CLOSE
          </button>
        </div>
      )}

      {/* 상단 고정 배너 (고객 화면 & 종료된 방) */}
      {!isAdmin && isCompleted && (
        <div className="shrink-0 flex justify-center items-center px-4 py-2.5 bg-[#111] border-b border-[#333] z-10">
          <span className="text-[10px] text-[#888] font-mono tracking-widest flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-[#555]"></span>
            상담이 종료된 방입니다.
          </span>
        </div>
      )}

      <div 
        ref={containerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto overflow-x-hidden p-4 space-y-4 custom-scrollbar"
      >

        {isLoading && (
          <div className="text-center py-2 text-chat-muted text-xs font-mono">
            Loading past communications...
          </div>
        )}
        
        {isError && (
          <div className="text-center py-3 my-2 text-xs font-mono rounded bg-chat-primary/10 border border-chat-border text-chat-primary-hover">
            통신이 불안정하여 일부 대화 기록을 불러오지 못했습니다.
          </div>
        )}

        {messages.map((m, idx) => (
          <ChatMessageBubble 
            key={idx}
            message={m}
            customerId={customerId}
            isAdmin={isAdmin}
            isCompleted={isCompleted}
            isLatest={idx === messages.length - 1}
            onSendBotOption={(label) => sendMessage(customerId, label)}
          />
        ))}
      </div>

      {showScrollBottom && (
        <button
          onClick={scrollToBottom}
          className="absolute bottom-14 left-1/2 -translate-x-1/2 px-4 py-1.5 rounded-full flex items-center justify-center gap-1.5 transition-all hover:bg-black shadow-[0_4px_10px_rgba(0,0,0,0.5)] z-50 text-[10px] font-bold tracking-widest uppercase cursor-pointer bg-chat-dark/95 border border-chat-border-light text-[#ff4422] font-mono"
        >
          <span>Latest</span>
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </button>
      )}

      <form onSubmit={send} className="p-3 border-t flex gap-2 border-chat-border bg-[#0a0000]">
        <input 
          type="text" 
          value={input}
          onChange={(e) => setInput(e.target.value)}
          disabled={isInputDisabled}
          placeholder={
            isCompleted ? "상담이 완전히 종료되었습니다." 
            : isAdminBotMode ? "봇 모드에서는 채팅을 입력할 수 없습니다."
            : "메시지 전송..."
          }
          className={`flex-1 bg-black border rounded px-3 py-2 text-sm text-white focus:outline-none focus:border-chat-primary transition-colors border-chat-primary/25 ${isInputDisabled ? 'opacity-50' : 'opacity-100'}`}
        />
        <button 
          type="submit" 
          disabled={isInputDisabled || !input.trim()}
          className={`px-4 py-2 font-bold rounded text-xs transition-colors hover:bg-chat-primary-hover disabled:hover:bg-[#333] font-mono ${isInputDisabled ? 'bg-[#333] text-[#888]' : 'bg-chat-primary text-white'}`}
        >
          SEND
        </button>
      </form>
    </div>
  );
}
