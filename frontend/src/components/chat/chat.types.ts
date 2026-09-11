export type ChatRoomStatus = "WAITING" | "IN_PROGRESS" | "COMPLETED";

export interface ChatRoomResponse {
  roomId: number;
  title: string;
  status: ChatRoomStatus;
  createdAt: string;
}

export type ChatMessageType = "TEXT" | "SYSTEM" | "BUTTON";

export interface ChatMessageResponse {
  roomId: number;
  memberId: number;
  content: string;
  messageType?: ChatMessageType;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  empty: boolean;
  first: boolean;
  last: boolean;
  number: number;
  numberOfElements: number;
}

export interface BotOptionDto {
  label: string;
  action: string;
}

export interface BotProductDto {
  id: string;
  name: string;
  price: number;
}

export interface BotMessageDto {
  text: string;
  options?: BotOptionDto[];
  products?: BotProductDto[];
}
