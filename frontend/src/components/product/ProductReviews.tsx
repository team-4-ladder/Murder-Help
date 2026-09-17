import { useEffect, useMemo, useState } from "react";
import {
    fetchProductReviews,
    type ProductReview,
} from "../../api/reviews";
import { C } from "../../lib/theme";

type ProductReviewsProps = {
    productId: number;
};

const RATINGS = [5, 4, 3, 2, 1] as const;

function formatDate(value: string) {
    if (!value) {
        return "";
    }

    return value
        .slice(0, 10)
        .replace(/-/g, ".");
}

function Stars({ rating }: { rating: number }) {
    return (
        <span
            aria-label={`별점 ${rating}점`}
            style={{
                fontFamily: "Share Tech Mono",
                letterSpacing: 2,
            }}
        >
      {RATINGS.map((star) => (
          <span
              key={star}
              style={{
                  color:
                      star <= rating
                          ? C.redBright
                          : C.textMuted,
              }}
          >
          ★
        </span>
      ))}
    </span>
    );
}

export function ProductReviews({
                                   productId,
                               }: ProductReviewsProps) {
    const [reviews, setReviews] = useState<ProductReview[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const controller = new AbortController();

        async function loadReviews() {
            setLoading(true);
            setError("");
            setReviews([]);

            try {
                const result = await fetchProductReviews(
                    productId,
                    controller.signal,
                );

                setReviews(result);
            } catch (error) {
                if (controller.signal.aborted) {
                    return;
                }

                setError(
                    error instanceof Error
                        ? error.message
                        : "리뷰를 불러오지 못했습니다.",
                );
            } finally {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            }
        }

        void loadReviews();

        return () => {
            controller.abort();
        };
    }, [productId]);

    const averageRating = useMemo(() => {
        if (reviews.length === 0) {
            return 0;
        }

        const totalRating = reviews.reduce(
            (sum, review) => sum + review.rating,
            0,
        );

        return totalRating / reviews.length;
    }, [reviews]);

    const ratingCounts = useMemo(() => {
        return RATINGS.reduce<Record<number, number>>(
            (counts, rating) => {
                counts[rating] = reviews.filter(
                    (review) => review.rating === rating,
                ).length;

                return counts;
            },
            {},
        );
    }, [reviews]);

    return (
        <section
            id="product-reviews"
            className="mt-8 p-5 md:p-8"
            style={{
                background: C.panel,
                border: `1px solid ${C.panelBorder}`,
            }}
        >
            {/* 리뷰 제목과 평균 별점 */}
            <div
                className="flex flex-wrap items-center gap-5 pb-6"
                style={{
                    borderBottom: `1px solid ${C.panelBorder}`,
                }}
            >
                <h2
                    className="text-xl"
                    style={{
                        color: C.text,
                        fontFamily: "Noto Sans KR, sans-serif",
                    }}
                >
                    고객 리뷰 {loading ? "" : `${reviews.length}건`}
                </h2>

                {!loading && reviews.length > 0 && (
                    <div className="flex items-center gap-2">
                        <Stars rating={Math.round(averageRating)} />

                        <strong
                            className="text-lg"
                            style={{
                                color: C.text,
                                fontFamily: "Share Tech Mono",
                            }}
                        >
                            {averageRating.toFixed(1)}
                        </strong>
                    </div>
                )}
            </div>

            {/* 로딩 상태 */}
            {loading && (
                <div
                    className="py-16 text-center text-sm"
                    style={{
                        color: C.textDim,
                        fontFamily: "Noto Sans KR, sans-serif",
                    }}
                >
                    리뷰를 불러오는 중입니다.
                </div>
            )}

            {/* API 오류 */}
            {!loading && error && (
                <div
                    className="py-16 text-center text-sm"
                    style={{
                        color: C.redBright,
                        fontFamily: "Noto Sans KR, sans-serif",
                    }}
                >
                    {error}
                </div>
            )}

            {/* 등록된 리뷰가 없는 경우 */}
            {!loading && !error && reviews.length === 0 && (
                <div className="py-20 text-center">
                    <p
                        className="text-base mb-2"
                        style={{
                            color: C.textDim,
                            fontFamily: "Noto Sans KR, sans-serif",
                        }}
                    >
                        아직 등록된 리뷰가 없습니다.
                    </p>

                    <p
                        className="text-xs"
                        style={{
                            color: C.textMuted,
                            fontFamily: "Share Tech Mono",
                        }}
                    >
                        이 상품의 첫 번째 리뷰를 작성해 주세요.
                    </p>
                </div>
            )}

            {/* 등록된 리뷰가 있는 경우 */}
            {!loading && !error && reviews.length > 0 && (
                <>
                    {/* 별점별 분포 */}
                    <div
                        className="py-7"
                        style={{
                            borderBottom: `1px solid ${C.panelBorder}`,
                        }}
                    >
                        {RATINGS.map((rating) => {
                            const count =
                                ratingCounts[rating] ?? 0;

                            const percentage =
                                reviews.length > 0
                                    ? (count / reviews.length) * 100
                                    : 0;

                            return (
                                <div
                                    key={rating}
                                    className="flex items-center gap-4 mb-3 last:mb-0"
                                >
                  <span
                      className="w-8 text-xs"
                      style={{
                          color: C.textDim,
                          fontFamily: "Share Tech Mono",
                      }}
                  >
                    {rating}★
                  </span>

                                    <div
                                        className="flex-1 h-2 overflow-hidden"
                                        style={{
                                            background:
                                                "rgba(255,255,255,0.07)",
                                        }}
                                    >
                                        <div
                                            className="h-full transition-all"
                                            style={{
                                                width: `${percentage}%`,
                                                background: C.red,
                                            }}
                                        />
                                    </div>

                                    <span
                                        className="w-8 text-right text-xs"
                                        style={{
                                            color: C.textMuted,
                                            fontFamily: "Share Tech Mono",
                                        }}
                                    >
                    {count}
                  </span>
                                </div>
                            );
                        })}
                    </div>

                    {/* 리뷰 목록 */}
                    <div>
                        {reviews.map((review) => (
                            <article
                                key={review.reviewId}
                                className="py-6"
                                style={{
                                    borderBottom:
                                        `1px solid ${C.panelBorder}`,
                                }}
                            >
                                <div className="flex flex-wrap items-center gap-3 mb-3">
                                    <Stars rating={review.rating} />

                                    <span
                                        className="text-xs"
                                        style={{
                                            color: C.textDim,
                                            fontFamily:
                                                "Noto Sans KR, sans-serif",
                                        }}
                                    >
                    구매자*****
                  </span>

                                    <span
                                        className="text-xs"
                                        style={{
                                            color: C.textMuted,
                                            fontFamily: "Share Tech Mono",
                                        }}
                                    >
                    {formatDate(review.createdAt)}
                  </span>
                                </div>

                                <p
                                    className="text-sm leading-7 whitespace-pre-wrap break-words"
                                    style={{
                                        color: C.textDim,
                                        fontFamily:
                                            "Noto Sans KR, sans-serif",
                                    }}
                                >
                                    {review.content}
                                </p>
                            </article>
                        ))}
                    </div>
                </>
            )}
        </section>
    );
}