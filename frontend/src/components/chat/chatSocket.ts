import SockJS from "sockjs-client";
import { Client, type StompSubscription } from "@stomp/stompjs";
import { getAccessToken } from "../../api/client";

/* ─── 공유 STOMP 커넥션 ───────────────────────────────────────
   관리자 대시보드와 개별 채팅방 뷰가 각자 소켓을 열면 연결이 중복되므로,
   destination을 구독하는 컴포넌트가 몇 개든 실제 연결/구독은 하나만 유지한다.
   (frontend/src/api/client.ts 의 모듈 레벨 싱글턴 패턴과 동일한 방식) */

type MessageCallback = (body: string) => void;

let client: Client | null = null;
let refCount = 0;
let hasConnectedOnce = false;

const registrations = new Map<string, Set<MessageCallback>>();
const liveSubscriptions = new Map<string, StompSubscription>();
const reconnectListeners = new Set<() => void>();

function subscribeDestination(destination: string) {
  if (!client) return;
  liveSubscriptions.get(destination)?.unsubscribe();
  const subscription = client.subscribe(destination, (msg) => {
    registrations.get(destination)?.forEach((callback) => callback(msg.body));
  });
  liveSubscriptions.set(destination, subscription);
}

function ensureClient(): Client {
  if (client) return client;

  client = new Client({
    webSocketFactory: () => new SockJS("/ws"),
    connectHeaders: {
      Authorization: `Bearer ${getAccessToken()}`
    },
    reconnectDelay: 5000,
    onConnect: () => {
      const isReconnect = hasConnectedOnce;
      hasConnectedOnce = true;

      registrations.forEach((_callbacks, destination) => subscribeDestination(destination));

      if (isReconnect) {
        reconnectListeners.forEach((listener) => listener());
      }
    }
  });

  client.activate();
  return client;
}

/** destination을 구독한다. 반환된 함수를 호출하면 구독을 해제한다. */
export function subscribeChatSocket(destination: string, callback: MessageCallback): () => void {
  refCount += 1;
  const activeClient = ensureClient();

  if (!registrations.has(destination)) {
    registrations.set(destination, new Set());
  }
  registrations.get(destination)!.add(callback);

  if (activeClient.connected) {
    subscribeDestination(destination);
  }

  return () => {
    const callbacks = registrations.get(destination);
    callbacks?.delete(callback);
    if (callbacks && callbacks.size === 0) {
      registrations.delete(destination);
      liveSubscriptions.get(destination)?.unsubscribe();
      liveSubscriptions.delete(destination);
    }

    refCount = Math.max(0, refCount - 1);
    if (refCount === 0) {
      client?.deactivate();
      client = null;
      hasConnectedOnce = false;
      liveSubscriptions.clear();
    }
  };
}

/** 최초 연결이 아닌 "재연결"이 발생했을 때 알림을 받는다. */
export function onChatSocketReconnect(callback: () => void): () => void {
  reconnectListeners.add(callback);
  return () => {
    reconnectListeners.delete(callback);
  };
}

export function isChatSocketConnected(): boolean {
  return client?.connected ?? false;
}

export function publishChatSocket(destination: string, body: string) {
  client?.publish({ destination, body });
}
