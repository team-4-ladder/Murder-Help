package org.example.murderhelp.domain.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.scheduler.ChatbotRankingScheduler;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/product-rankings")
public class ProductRankingAdminController {

    private final ChatbotRankingScheduler chatbotRankingScheduler;

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('GREEN')")
    public ApiResponse<Void> refresh() {
        chatbotRankingScheduler.runWeeklyBestUpdate();
        return ApiResponse.ok();
    }
}
