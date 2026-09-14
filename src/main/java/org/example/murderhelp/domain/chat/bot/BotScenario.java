package org.example.murderhelp.domain.chat.bot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.bot.dto.BotMessageDto;
import org.example.murderhelp.domain.chat.bot.dto.BotOptionDto;
import org.example.murderhelp.domain.chat.bot.dto.BotProductDto;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum BotScenario {
    
    WELCOME(
        BotMessageDto.builder()
            .text("MurderHelp 고객 지원 센터에 오신 것을 환영합니다.\n원하시는 항목을 선택해 주세요.")
            .options(Constants.MAIN_MENU_OPTIONS)
            .build()
    ),
    
    RECOMMEND_WEAPON(
        BotMessageDto.builder()
            .text("고객님의 등급에 맞는 추천 무기 리스트입니다.")
            .options(Constants.RETURN_MENU_OPTIONS)
            .products(List.of(
                new BotProductDto("w-001", "소음기 장착 권총", 1500),
                new BotProductDto("w-002", "저격 라이플", 3000)
            ))
            .build()
    ),

    FALLBACK(
        BotMessageDto.builder()
            .text("버튼을 통해 원하시는 항목을 선택해 주세요.")
            .options(Constants.MAIN_MENU_OPTIONS)
            .build()
    );

    private final BotMessageDto messageDto;

    public static class Constants {
        public static final List<BotOptionDto> MAIN_MENU_OPTIONS = List.of(
            BotCommand.CONNECT_AGENT.toOption(),
            BotCommand.RECOMMEND_WEAPON.toOption(),
            BotCommand.CHECK_ORDER.toOption()
        );

        public static final List<BotOptionDto> RETURN_MENU_OPTIONS = List.of(
            BotCommand.CONNECT_AGENT.toOption(),
            BotCommand.BACK_TO_MAIN.toOption()
        );
    }
}
