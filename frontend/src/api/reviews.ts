import { authFetch } from "./client";

type ApiResponse<T> = {
    code: string;
    message?: string;
    data?: T;
};

export type ProductReview = {
    reviewId: number;
    orderItemId: number;
    productId: number;
    rating: number;
    content: string;
    createdAt: string;
    updatedAt: string;
};

/**
 * 특정 상품에 등록된 리뷰를 최신순으로 조회한다.
 *
 * GET /api/products/{productId}/reviews
 */
export async function fetchProductReviews(
    productId: number,
    signal?: AbortSignal,
): Promise<ProductReview[]> {
    const response = await authFetch(
        `/api/products/${productId}/reviews`,
        {
            method: "GET",
            headers: {
                Accept: "application/json",
            },
            signal,
        },
    );

    const body = (await response.json().catch(() => null)) as
        | ApiResponse<ProductReview[]>
        | null;

    if (
        !response.ok ||
        !body ||
        body.code !== "SUCCESS"
    ) {
        throw new Error(
            body?.message ??
            `리뷰를 불러오지 못했습니다. (${response.status})`,
        );
    }

    return body.data ?? [];
}