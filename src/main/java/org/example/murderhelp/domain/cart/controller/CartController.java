package org.example.murderhelp.domain.cart.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.cart.dto.CartItemAddRequest;
import org.example.murderhelp.domain.cart.dto.CartItemDetailResponse;
import org.example.murderhelp.domain.cart.dto.CartItemUpdateRequest;
import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.cart.dto.CartItemsDeleteRequest;
import org.example.murderhelp.domain.cart.service.CartService;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cart/items")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemResponse>> addItem(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.addItem(memberId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemDetailResponse>>> getItems(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getItems(memberId)));
    }

    @PatchMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateItemQuantity(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.updateItemQuantity(memberId, cartItemId, request)));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long cartItemId
    ) {
        cartService.deleteItem(memberId, cartItemId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteItems(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CartItemsDeleteRequest request
    ) {
        cartService.deleteItems(memberId, request.cartItemIds());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
