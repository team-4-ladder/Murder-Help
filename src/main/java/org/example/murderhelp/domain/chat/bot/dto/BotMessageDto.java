package org.example.murderhelp.domain.chat.bot.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record BotMessageDto(
    String title,
    String text,
    Long orderId,
    String orderStatus,
    List<BotOptionDto> options,
    List<BotProductDto> products
) {}
