export type ChatRoomStatus = "WAITING" | "IN_PROGRESS" | "COMPLETED";

export interface ChatRoomResponse {
  roomId: number;
  title: string;
  status: ChatRoomStatus;
  createdAt: string;
  updatedAt: string;
  lastMessage?: string;
  customerProfileImageUrl?: string | null;
}

export type ChatMessageType = "TEXT" | "SYSTEM" | "BUTTON";

export interface ChatMessageResponse {
  id: number;
  roomId: number;
  memberId: number;
  senderEmail: string;
  senderName: string;
  senderGrade?: string;
  senderProfileImageUrl?: string | null;
  content: string;
  messageType: "TEXT" | "SYSTEM" | "BUTTON";
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  empty: boolean;
  first: boolean;
  last: boolean;
  number: number;
  numberOfElements: number;
  totalElements: number;
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
  title?: string;
  text?: string;
  options?: BotOptionDto[];
  products?: BotProductDto[];
}
