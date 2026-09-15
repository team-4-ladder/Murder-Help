import { useEffect, useState } from "react";
import type { ProductDetailData } from "../../api/products";
import { C, krw } from "../../lib/theme";
import { TIERS } from "../../lib/tier";
import { QtyStepper } from "../common/QtyStepper";
import { TierBadge } from "../member/TierBadge";
import { ProductReviews } from "./ProductReviews";

/* ─── product detail ─────────────────────────────────────── */
export function ProductDetail({
                                p,
                                onBack,
                                onAddToCart,
                              }: {
  p: ProductDetailData;
  onBack: () => void;

  /* 서버 장바구니에 담긴 경우 true를 반환한다. */
  onAddToCart: (qty: number) => Promise<boolean>;
}) {
  const [qty, setQty] = useState(1);
  const [added, setAdded] = useState(false);
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState("");

  const tier = TIERS[p.tier];

  /*
   * 재고가 0이 되면 서버가 SOLD_OUT으로 변경하지만,
   * 둘 중 하나만 해당해도 프론트에서는 품절로 처리한다.
   */
  const soldOut =
      p.status === "SOLD_OUT" ||
      p.stockQuantity <= 0;

  const maxQty = Math.min(99, p.stockQuantity);

  useEffect(() => {
    setQty(1);
    setAdded(false);
    setAdding(false);
    setAddError("");

    window.scrollTo({
      top: 0,
    });
  }, [p.productId]);

  async function add() {
    if (adding || soldOut) {
      return;
    }

    setAdding(true);
    setAddError("");

    try {
      const success = await onAddToCart(qty);

      if (!success) {
        return;
      }

      setAdded(true);

      window.setTimeout(() => {
        setAdded(false);
      }, 1800);
    } catch (error) {
      setAddError(
          error instanceof Error
              ? error.message
              : "장바구니에 상품을 담지 못했습니다.",
      );
    } finally {
      setAdding(false);
    }
  }

  function moveToReviews() {
    document
        .getElementById("product-reviews")
        ?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
  }

  return (
      <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-6">
        {/* 목록으로 돌아가기 */}
        <button
            type="button"
            onClick={onBack}
            className="text-xs uppercase tracking-widest mb-4"
            style={{
              color: C.textDim,
              fontFamily: "Share Tech Mono",
            }}
        >
          ← 목록으로
        </button>

        {/* 상품 상세 영역 */}
        <div
            className="flex flex-col md:flex-row gap-0"
            style={{
              background: C.panel,
              border: `1px solid ${C.panelBorder}`,
            }}
        >
          {/* 상품 이미지 */}
          <div
              className="md:w-[52%] shrink-0 relative"
              style={{
                background: "#060606",
              }}
          >
            <img
                src={p.img}
                alt={p.name}
                className="w-full object-cover"
                style={{
                  height: 420,
                  filter: "brightness(0.85) saturate(0.7)",
                }}
            />

            {p.badge && (
                <span
                    className="absolute top-3 left-3 text-[10px] font-black uppercase tracking-widest px-2 py-1"
                    style={{
                      background:
                          p.badge === "NEW"
                              ? "#1a7a3a"
                              : C.red,
                      color: "#fff",
                      fontFamily: "Share Tech Mono",
                    }}
                >
              {p.badge}
            </span>
            )}

            <div
                className="absolute bottom-0 left-0 right-0"
                style={{
                  height: 3,
                  background: tier.color,
                }}
            />
          </div>

          {/* 상품 정보 */}
          <div className="flex-1 p-6 md:p-8">
            {/* 카테고리 및 상품 코드 */}
            <div
                className="flex items-center gap-2 mb-3 text-[10px] uppercase tracking-widest"
                style={{
                  color: C.textMuted,
                  fontFamily: "Share Tech Mono",
                }}
            >
              <span>{p.category}</span>

              <span style={{ color: C.redDim }}>
              /
            </span>

              <span>{p.sub}</span>

              <span style={{ color: C.redDim }}>
              /
            </span>

              <span>item No. {p.id}</span>
            </div>

            {/* 회원 등급 */}
            <div className="mb-3">
              <TierBadge tier={p.tier} />
            </div>

            {/* 상품명 */}
            <h1
                className="text-2xl leading-snug mb-4"
                style={{
                  color: C.text,
                  fontFamily:
                      "Noto Sans KR, sans-serif",
                  fontWeight: 300,
                }}
            >
              {p.name}
            </h1>

            {/* 가격 및 품절 상태 */}
            <div className="flex items-baseline gap-3 mb-5">
            <span
                className="text-3xl font-bold"
                style={{
                  color: tier.brightColor,
                  fontFamily: "Share Tech Mono",
                }}
            >
              {krw(p.price)}
            </span>

              {soldOut && (
                  <span
                      className="text-xs"
                      style={{
                        color: C.redBright,
                        fontFamily: "Share Tech Mono",
                      }}
                  >
                SOLD OUT · 품절
              </span>
              )}
            </div>

            {/* 상품 설명 */}
            <p
                className="text-sm leading-relaxed mb-6"
                style={{
                  color: C.textDim,
                  fontFamily:
                      "Noto Sans KR, sans-serif",
                  fontWeight: 300,
                }}
            >
              {p.desc ||
                  "상세 설명은 준비 중입니다."}
            </p>

            {/* 상품 제원 */}
            <div
                className="mb-7"
                style={{
                  borderTop: `1px solid ${C.panelBorder}`,
                }}
            >
              {p.specs.map(([label, value]) => (
                  <div
                      key={label}
                      className="flex gap-4 py-2.5"
                      style={{
                        borderBottom:
                            `1px solid ${C.panelBorder}`,
                      }}
                  >
                <span
                    className="text-[10px] uppercase tracking-widest shrink-0"
                    style={{
                      color: C.textMuted,
                      fontFamily:
                          "Share Tech Mono",
                      width: 92,
                      paddingTop: 2,
                    }}
                >
                  {label}
                </span>

                    <span
                        className="text-xs"
                        style={{
                          color: C.text,
                          fontFamily:
                              "Noto Sans KR, sans-serif",
                          fontWeight: 300,
                        }}
                    >
                  {value}
                </span>
                  </div>
              ))}
            </div>

            {/* 수량과 총금액 */}
            <div className="flex items-center gap-4 mb-4">
            <span
                className="text-[10px] uppercase tracking-widest"
                style={{
                  color: C.textMuted,
                  fontFamily: "Share Tech Mono",
                }}
            >
              수량
            </span>

              <QtyStepper
                  qty={qty}
                  onChange={setQty}
                  max={maxQty}
              />

              <span
                  className="text-sm ml-auto"
                  style={{
                    color: C.textDim,
                    fontFamily: "Share Tech Mono",
                  }}
              >
              합계{" "}
                <span
                    style={{
                      color: tier.brightColor,
                    }}
                >
                {krw(p.price * qty)}
              </span>
            </span>
            </div>

            {/* 리뷰보기와 장바구니 버튼 */}
            <div className="flex gap-3">
              <button
                  type="button"
                  onClick={moveToReviews}
                  className="flex-1 py-3.5 text-sm font-bold uppercase tracking-widest transition-all"
                  style={{
                    border:
                        `1px solid ${C.panelBorder}`,
                    color: C.text,
                    background: "rgba(0,0,0,0.4)",
                    fontFamily: "Share Tech Mono",
                  }}
              >
                리뷰보기
              </button>

              <button
                  type="button"
                  onClick={add}
                  disabled={adding || soldOut}
                  className="flex-1 py-3.5 text-sm font-bold uppercase tracking-widest transition-all"
                  style={{
                    background: soldOut
                        ? C.redDim
                        : C.red,
                    color: "#fff",
                    border: `1px solid ${
                        soldOut
                            ? C.redDim
                            : added
                                ? tier.brightColor
                                : C.redBright
                    }`,
                    fontFamily: "Share Tech Mono",
                    cursor: soldOut
                        ? "not-allowed"
                        : adding
                            ? "wait"
                            : "pointer",
                    opacity: soldOut ? 0.6 : 1,
                  }}
              >
                {soldOut
                    ? "품절된 상품입니다"
                    : adding
                        ? "담는 중..."
                        : added
                            ? "담았습니다 ✓"
                            : "장바구니에 담기 →"}
              </button>
            </div>

            {/* 장바구니 오류 */}
            {addError && (
                <p
                    className="text-xs mt-3"
                    style={{
                      color: C.redBright,
                      fontFamily:
                          "Noto Sans KR, sans-serif",
                    }}
                >
                  {addError}
                </p>
            )}

            {/* 이용 안내 */}
            <p
                className="text-[10px] mt-4"
                style={{
                  color: C.textMuted,
                  fontFamily:
                      "Noto Sans KR, sans-serif",
                }}
            >
              BB탄 전용 에어소프트 제품입니다.
              만 18세 이상만 구매하실 수 있습니다.
            </p>
          </div>
        </div>

        {/* 상품 리뷰 영역 */}
        <ProductReviews productId={p.productId} />
      </div>
  );
}