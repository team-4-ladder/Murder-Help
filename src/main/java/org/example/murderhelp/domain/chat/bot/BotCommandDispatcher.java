package org.example.murderhelp.domain.chat.bot;

import jakarta.annotation.PostConstruct;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.example.murderhelp.domain.order.service.OrderService;
import org.example.murderhelp.domain.order.dto.OrderListRequest;
import org.example.murderhelp.domain.order.dto.OrderListPeriod;
import org.example.murderhelp.domain.order.dto.OrderResponse;
import org.example.murderhelp.domain.chat.bot.dto.BotMessageDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
@lombok.RequiredArgsConstructor
public class BotCommandDispatcher {

    private final OrderService orderService;
    private final Map<BotCommand, BiConsumer<ChatRoom, ChatMessageService>> actionMap = new EnumMap<>(BotCommand.class);

    @PostConstruct
    public void init() {
        actionMap.put(BotCommand.CONNECT_AGENT, this::connectAgent);
        actionMap.put(BotCommand.RECOMMEND_WEAPON, (room, svc) -> svc.sendBotScenarioMessage(room, BotScenario.RECOMMEND_WEAPON));
        actionMap.put(BotCommand.CHECK_ORDER, this::handleCheckOrder);
        actionMap.put(BotCommand.BACK_TO_MAIN, (room, svc) -> svc.sendBotScenarioMessage(room, BotScenario.WELCOME));
        actionMap.put(BotCommand.UNKNOWN, (room, svc) -> svc.sendBotScenarioMessage(room, BotScenario.FALLBACK));
    }

    public void execute(ChatRoom room, String content, ChatMessageService chatMessageService) {
        BotCommand command = BotCommand.from(content);
        actionMap.getOrDefault(command, (r, svc) -> svc.sendBotScenarioMessage(r, BotScenario.FALLBACK))
                 .accept(room, chatMessageService);
    }

    private void connectAgent(ChatRoom room, ChatMessageService svc) {
        room.changeToWaiting();
        svc.sendSystemMessage(room, "상담사 연결을 대기 중입니다. 잠시만 기다려주세요.");
    }

    private void handleCheckOrder(ChatRoom room, ChatMessageService svc) {
        Page<OrderResponse> orders = orderService.getOrderList(
                room.getCustomer().getId(),
                new OrderListRequest(OrderListPeriod.MONTH_3, null),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        String text;
        if (orders.isEmpty()) {
            text = "최근 3개월간 결제하신 주문 내역이 없습니다.";
        } else {
            OrderResponse order = orders.getContent().get(0);
            String firstItemName = order.items().get(0).productName();
            int extraCount = order.items().size() - 1;
            String productTitle = extraCount > 0 ? String.format("%s 외 %d건", firstItemName, extraCount) : firstItemName;
            
            // 관리자님 요청대로 영문 상태값(Enum name) 그대로 노출
            text = String.format("고객님의 최근 주문 [%s]은(는) 현재 [%s] 상태입니다.", productTitle, order.status().name());
        }

        BotMessageDto messageDto = BotMessageDto.builder()
                .text(text)
                .options(BotScenario.Constants.RETURN_MENU_OPTIONS)
                .build();
        svc.sendBotMessage(room, messageDto);
    }
}
