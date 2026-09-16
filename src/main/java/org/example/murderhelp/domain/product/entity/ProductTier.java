package org.example.murderhelp.domain.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum ProductTier {

    YELLOW("yellow", 0),
    PURPLE("purple", 1),
    RED("red", 2),
    GREEN("green", 3);


    private final String value;
    private final int rank;

    public boolean canAccess(ProductTier requestedTier) {
        return requestedTier != null && requestedTier.rank <= this.rank;
    }

    public static ProductTier fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "상품 등급은 필수입니다.");
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "yellow" -> YELLOW;
            case "purple" -> PURPLE;
            case "red" -> RED;
            case "green" -> GREEN;
            default -> throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "지원하지 않는 상품 등급입니다: " + value
            );
        };
    }
}
