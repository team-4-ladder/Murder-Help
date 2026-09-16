import { useState } from "react";
import ChatRoomList from "./ChatRoomList";
import ChatRoomView from "./ChatRoomView";

export default function ChatModal({ customerId, onClose }: { customerId: number; onClose: () => void }) {
  const [activeRoomId, setActiveRoomId] = useState<number | null>(null);
  // 뒤로 가기를 눌렀을 때 다시 강제 진입하는 것을 막기 위한 상태
  const [preventAutoJoin, setPreventAutoJoin] = useState(false);

  return (
    <div 
      className="fixed bottom-28 right-8 w-[380px] h-[550px] flex flex-col shadow-[0_0_30px_rgba(0,0,0,1)] rounded-lg overflow-hidden z-50 transition-all bg-[#0a0000]/95 border border-chat-border-light"
    >
      {/* Header */}
      <div className="px-4 py-3 flex justify-between items-center border-b border-chat-border bg-chat-dark">
        <div className="flex items-center gap-2">
          {activeRoomId && (
            <button 
              onClick={() => {
                setActiveRoomId(null);
                setPreventAutoJoin(true); // 뒤로 가기 누름 표시
              }}
              className="text-gray-400 hover:text-white transition-colors text-lg mr-1"
            >
              {"<"}
            </button>
          )}
          <h3 className="text-white font-bold tracking-widest text-sm uppercase" style={{ fontFamily: "Share Tech Mono, monospace" }}>
            1:1 SUPPORT
          </h3>
        </div>
        <button onClick={onClose} className="text-gray-500 hover:text-white font-bold transition-colors">✕</button>
      </div>

      {/* Body: List or Room View */}
      <div className="flex-1 overflow-hidden relative">
        {activeRoomId ? (
          <ChatRoomView key={activeRoomId} roomId={activeRoomId} customerId={customerId} />
        ) : (
          <ChatRoomList customerId={customerId} onSelectRoom={setActiveRoomId} preventAutoJoin={preventAutoJoin} />
        )}
      </div>
    </div>
  );
}
