import { useState } from "react";
import { C } from "../../lib/theme";
import { PageTitle } from "../common/PageTitle";

/* ─── 마이페이지 ─────────────────────────────────────────── */
/* 지금은 메뉴 두 개만 있다. 각 메뉴의 실제 화면은 아직 없다. */
const MYPAGE_MENUS = [
    { key: "orders", label: "내 주문내역 관리", desc: "주문 조회 · 배송 상태 · 취소" },
    { key: "reviews", label: "내 리뷰 관리", desc: "작성한 리뷰 확인 · 수정 · 삭제" },
];

type ReviewTab = "pending" | "written";

type ReviewItem = {
    id: number;
    productCode: string;
    productName: string;
    purchasedAt: string;
    content?: string;
    rating?: number;
};

const INITIAL_PENDING: ReviewItem[] = [
    { id: 1, productCode: "MG004", productName: "기관단총 Delta", purchasedAt: "2026.08.01" },
    { id: 2, productCode: "MG011", productName: "스나이퍼라이플 Ghost", purchasedAt: "2026.07.20" },
];

const INITIAL_WRITTEN: ReviewItem[] = [
    { id: 3, productCode: "HG002", productName: "리볼버 Raven", purchasedAt: "2026.08.21", rating: 5, content: "탄속이 생각보다 강력하고 마감도 꼼꼼합니다. 재구매 의사 있어요." },
    { id: 4, productCode: "AR008", productName: "어설트라이플 Phantom", purchasedAt: "2026.08.15", rating: 4, content: "무게감이 좋고 조립도 쉬웠습니다. 다만 탄창이 조금 뻑뻑한 느낌이에요." },
];

export function MyPage({ onBack }: { onBack: () => void }) {
    const [section, setSection] = useState<"menu" | "reviews">("menu");
    const [reviewTab, setReviewTab] = useState<ReviewTab>("pending");
    const [pendingReviews, setPendingReviews] = useState(INITIAL_PENDING);
    const [writtenReviews, setWrittenReviews] = useState(INITIAL_WRITTEN);

    function writeReview(review: ReviewItem) {
        setPendingReviews((items) => items.filter((item) => item.id !== review.id));
        setWrittenReviews((items) => [
            { ...review, purchasedAt: new Date().toISOString().slice(0, 10).replace(/-/g, "."), rating: 5, content: "상품을 구매하고 작성한 리뷰입니다." },
            ...items,
        ]);
        setReviewTab("written");
    }

    function deleteReview(review: ReviewItem) {
        setWrittenReviews((items) => items.filter((item) => item.id !== review.id));
        setPendingReviews((items) => [...items, { ...review, content: undefined, rating: undefined }]);
        setReviewTab("pending");
    }

    if (section === "reviews") {
        return (
            <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-8">
                <button
                    onClick={() => setSection("menu")}
                    className="text-xs uppercase tracking-widest mb-4"
                    style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}
                >
                    ← My Page
                </button>

                <PageTitle note="// 구매 확정 상품의 리뷰를 작성하고 관리합니다">My Review Management</PageTitle>

                <div className="max-w-4xl" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
                    <div className="grid grid-cols-2" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
                        {([
                            ["pending", "리뷰 작성"],
                            ["written", "작성한 리뷰"],
                        ] as const).map(([tab, label]) => {
                            const active = reviewTab === tab;
                            return (
                                <button
                                    key={tab}
                                    onClick={() => setReviewTab(tab)}
                                    className="py-4 text-sm transition-colors"
                                    style={{
                                        color: active ? C.text : C.textMuted,
                                        fontFamily: "Noto Sans KR, sans-serif",
                                        borderBottom: `2px solid ${active ? C.red : "transparent"}`,
                                        background: active ? "rgba(255,255,255,0.025)" : "transparent",
                                    }}
                                >
                                    {label}
                                </button>
                            );
                        })}
                    </div>

                    {reviewTab === "pending" && (
                        <div className="px-5 md:px-8">
                            {pendingReviews.length === 0 ? (
                                <EmptyReviewState text="작성할 수 있는 리뷰가 없습니다." />
                            ) : pendingReviews.map((review) => (
                                <div key={review.id} className="flex gap-4 items-center py-6" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
                                    <div className="flex-1 min-w-0">
                                        <div className="text-sm" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
                                            {review.productCode} <span style={{ color: C.textMuted }}>·</span> <span style={{ fontFamily: "Noto Sans KR, sans-serif" }}>{review.productName}</span>
                                        </div>
                                        <div className="mt-2 text-xs" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
                                            {review.purchasedAt} 구매 확정
                                        </div>
                                    </div>
                                    <button onClick={() => writeReview(review)} className="px-5 py-2 text-xs shrink-0" style={{ background: C.red, color: "#fff", fontFamily: "Noto Sans KR, sans-serif" }}>
                                        리뷰 쓰기
                                    </button>
                                </div>
                            ))}
                            <p className="py-6 text-xs" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
                                구매 확정된 상품만 리뷰 작성 대기 목록에 노출됩니다.
                            </p>
                        </div>
                    )}

                    {reviewTab === "written" && (
                        <div className="px-5 md:px-8">
                            {writtenReviews.length === 0 ? (
                                <EmptyReviewState text="작성한 리뷰가 없습니다." />
                            ) : writtenReviews.map((review) => (
                                <div key={review.id} className="flex gap-4 items-start py-6" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
                                    <div className="flex-1 min-w-0">
                                        <div className="text-sm" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
                                            {"★".repeat(review.rating ?? 5)} <span style={{ color: C.textMuted }}> {review.productCode} · </span><span style={{ fontFamily: "Noto Sans KR, sans-serif" }}>{review.productName}</span>
                                        </div>
                                        <p className="mt-2 text-xs leading-relaxed" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif" }}>{review.content}</p>
                                        <div className="mt-2 text-[11px]" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>{review.purchasedAt}</div>
                                    </div>
                                    <div className="flex gap-2 shrink-0">
                                        <button className="px-3 py-1.5 text-xs" style={{ border: `1px solid ${C.panelBorder}`, color: C.textDim }}>수정</button>
                                        <button onClick={() => deleteReview(review)} className="px-3 py-1.5 text-xs" style={{ border: `1px solid ${C.panelBorder}`, color: C.textMuted }}>삭제</button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-8">
            <button
                onClick={onBack}
                className="text-xs uppercase tracking-widest mb-4"
                style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}
            >
                ← 목록으로
            </button>

            <PageTitle note="// 회원 정보와 활동 내역">My Page</PageTitle>

            <div className="max-w-2xl" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
                {MYPAGE_MENUS.map((menu) => (
                    <button
                        key={menu.key}
                        type="button"
                        onClick={() => menu.key === "reviews" && setSection("reviews")}
                        className="flex items-center gap-4 px-6 py-5"
                        style={{ borderBottom: `1px solid ${C.panelBorder}`, textAlign: "left", width: "100%", cursor: menu.key === "reviews" ? "pointer" : "default" }}
                    >
                        <div className="flex-1">
                            <div className="text-base mb-1" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
                                {menu.label}
                            </div>
                            <div className="text-xs" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
                                {menu.desc}
                            </div>
                        </div>
                        <span
                            className="text-[10px] uppercase tracking-widest px-2 py-1 shrink-0"
                            style={{ border: `1px solid ${C.panelBorder}`, color: C.textMuted, fontFamily: "Share Tech Mono" }}
                        >
              {menu.key === "reviews" ? "관리" : "준비 중"}
            </span>
                    </button>
                ))}
            </div>
        </div>
    );
}

function EmptyReviewState({ text }: { text: string }) {
    return <p className="py-12 text-center text-sm" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>{text}</p>;
}