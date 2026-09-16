import { useEffect, useState, type ReactNode } from "react";
import { fetchOrder, type OrderData, type OrderStatus } from "../../api/orders";
import { fetchRefundHistory, type RefundHistory } from "../../api/refund";
import { formatDate, formatDateTime, ORDER_STATUS_LABEL, orderStatusStyle } from "../../lib/order";
import { C, krw } from "../../lib/theme";
import { Spinner } from "../common/Spinner";
import { OrderItemRow } from "./OrderItemRow";
import { RefundModal } from "./RefundModal";

const STEPS: OrderStatus[] = ["PAID", "PREPARING_DELIVERY", "SHIPPING", "DELIVERED"];

function canRequestRefund(status: OrderStatus) {
  return status !== "PENDING_PAYMENT" && status !== "CANCELED";
}

const REFUND_STATUS_LABEL: Record<RefundHistory["status"], string> = {
  COMPLETED: "환불 완료",
  PG_FAILED: "환불 처리 지연 (확인 중)",
};

export function OrderDetail({
                              orderId,
                              onBack,
                              onOrderUpdated,
                            }: {
  orderId: number;
  onBack: () => void;
  /* 환불로 주문이 바뀌었을 때 — 바깥 목록도 다시 불러오도록 알린다 */
  onOrderUpdated?: () => void;
}) {
  const [order, setOrder] = useState<OrderData | null>(null);
  const [refunds, setRefunds] = useState<RefundHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [showRefund, setShowRefund] = useState(false);

  /* 상세 화면에 들어오면 목록에서 받아 둔 값을 쓰지 않고 상세 API 로 새로 조회한다.
     환불 내역은 주문에 딸린 paymentId 가 있어야 부를 수 있어서 주문을 받은 뒤에 이어서 부른다 */
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");

    async function load() {
      const detail = await fetchOrder(orderId, controller.signal);
      if (controller.signal.aborted) return;
      setOrder(detail);

      /* 환불 내역은 없어도 상세는 보여 줘야 하니 실패해도 넘어간다 */
      const history = await fetchRefundHistory(detail.paymentId).catch(() => []);
      if (!controller.signal.aborted) setRefunds(history);
    }

    load()
        .catch((error: unknown) => {
          if (error instanceof DOMException && error.name === "AbortError") return;
          setOrder(null);
          setError(error instanceof Error ? error.message : "주문 정보를 불러오지 못했습니다.");
        })
        .finally(() => {
          if (!controller.signal.aborted) setLoading(false);
        });

    return () => controller.abort();
  }, [orderId, reloadKey]);

  /* 환불하면 주문 상태·금액이 바뀌므로 상세를 통째로 다시 조회한다 */
  function refreshAfterRefund() {
    setShowRefund(false);
    setReloadKey((key) => key + 1);
    onOrderUpdated?.();
  }

  const backButton = (
      <button type="button" onClick={onBack} className="text-xs mb-5" style={{ color: C.textMuted }}>
        ← 주문 내역으로
      </button>
  );

  if (loading) {
    return (
        <>
          {backButton}
          <div
              className="flex items-center justify-center gap-3 py-16"
              style={{ border: `1px dashed ${C.panelBorder}`, color: C.textDim }}
          >
            <Spinner color={C.redBright} />
            <span className="text-xs" style={{ fontFamily: "Share Tech Mono" }}>
            주문 정보를 불러오는 중...
          </span>
          </div>
        </>
    );
  }

  if (!order) {
    return (
        <>
          {backButton}
          <div
              className="flex items-center justify-between gap-4 px-4 py-3"
              style={{ border: `1px solid ${C.redDim}` }}
          >
            <p className="text-xs" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
              {error || "주문 정보를 불러오지 못했습니다."}
            </p>
            <button
                type="button"
                onClick={() => setReloadKey((key) => key + 1)}
                className="shrink-0 px-3 py-1.5 text-[10px] font-bold uppercase tracking-widest"
                style={{ color: C.text, border: `1px solid ${C.panelBorder}`, fontFamily: "Share Tech Mono" }}
            >
              다시 시도
            </button>
          </div>
        </>
    );
  }

  const itemsTotal = order.items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
  const quantityTotal = order.items.reduce((sum, item) => sum + item.quantity, 0);
  const canceled = order.status === "CANCELED";

  return (
      <>
        {backButton}

        <div style={{ border: `1px solid ${C.panelBorder}`, background: "rgba(0,0,0,0.18)" }}>
          <section className="px-6 py-5" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
            <div className="flex flex-wrap items-center gap-3">
              <h1
                  className="font-bold uppercase leading-none"
                  style={{ fontFamily: "Cinzel, serif", fontSize: "clamp(18px,2.4vw,26px)", color: C.text }}
              >
                Order Detail
              </h1>
              <span className="text-xs" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
              {order.orderNumber}
            </span>
              {order.orderedAt && (
                  <span className="text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
                {formatDateTime(order.orderedAt)}
              </span>
              )}
              <span
                  className="ml-auto text-[10px] px-2 py-0.5 tracking-widest"
                  style={{ ...orderStatusStyle(order.status), fontFamily: "Share Tech Mono" }}
              >
              {ORDER_STATUS_LABEL[order.status]}
            </span>
            </div>

            {canceled ? (
                <p className="mt-5 text-sm" style={{ color: C.redBright, fontFamily: "Share Tech Mono" }}>
                  {order.canceledAt ? `${formatDate(order.canceledAt)} 취소됨` : "취소됨"}
                </p>
            ) : (
                <OrderProgress status={order.status} />
            )}
          </section>

          <section className="px-6 py-5" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
            <SectionLabel>주문 품목 {order.items.length}건</SectionLabel>
            {order.items.map((item, i) => (
                <div key={item.orderItemId ?? i} style={{ borderTop: i > 0 ? `1px solid ${C.panelBorder}` : undefined }}>
                  <OrderItemRow item={item} />
                </div>
            ))}
          </section>

          <div className="grid grid-cols-1 md:grid-cols-2" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
            <section className="px-6 py-5">
              <SectionLabel>배송 정보</SectionLabel>
              <InfoRow label="받는 사람">{order.receiverName}</InfoRow>
              <InfoRow label="연락처">{order.receiverPhone}</InfoRow>
              <InfoRow label="주소">{order.deliveryAddress}</InfoRow>
              <InfoRow label="배송 요청사항">{order.deliveryRequest || "-"}</InfoRow>
              <InfoRow label="송장번호">{order.trackingNumber || "-"}</InfoRow>
            </section>

            <section className="px-6 py-5 border-t md:border-t-0 md:border-l" style={{ borderColor: C.panelBorder }}>
              <SectionLabel>결제 정보</SectionLabel>
              <AmountRow label="상품 합계">{krw(itemsTotal)}</AmountRow>
              <AmountRow label="수량 합계">{quantityTotal}개</AmountRow>
              <AmountRow label="결제 수단">{order.paymentMethod || "-"}</AmountRow>

              <div className="flex items-center justify-between pt-4 mt-3" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
              <span className="text-sm font-bold" style={{ color: C.text }}>
                총 결제금액
              </span>
                <span
                    className="text-xl font-bold"
                    style={{ color: canceled ? C.textMuted : C.price, fontFamily: "Share Tech Mono" }}
                >
                {krw(order.totalAmount)}
              </span>
              </div>
            </section>
          </div>

          {refunds.length > 0 && (
              <section className="px-6 py-5" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
                <SectionLabel>환불 정보 {refunds.length}건</SectionLabel>
                {refunds.map((refund, i) => (
                    <div
                        key={refund.refundId}
                        className="py-3"
                        style={{ borderTop: i > 0 ? `1px solid ${C.panelBorder}` : undefined }}
                    >
                      <div className="flex items-center justify-between text-xs mb-2">
                  <span style={{ color: C.textMuted }}>
                    {formatDateTime(refund.refundDate)} · {REFUND_STATUS_LABEL[refund.status]}
                  </span>
                        <span style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
                    {krw(refund.pgRefundAmount)}
                  </span>
                      </div>
                      {refund.items.map((item) => (
                          <div
                              key={item.refundItemId}
                              className="flex items-center justify-between text-[11px] pl-3 py-0.5"
                              style={{ color: C.textDim }}
                          >
                    <span>
                      {item.productName} <span style={{ color: C.textMuted }}>× {item.refundQuantity}</span>
                    </span>
                            <span style={{ fontFamily: "Share Tech Mono" }}>{krw(item.itemRefundAmount)}</span>
                          </div>
                      ))}
                    </div>
                ))}
              </section>
          )}

          <div className="px-6 py-4 flex gap-3">
            <button
                type="button"
                onClick={onBack}
                className="px-6 py-2.5 text-xs"
                style={{ color: C.textDim, border: `1px solid ${C.panelBorder}` }}
            >
              목록으로
            </button>

            {canRequestRefund(order.status) && (
                <button
                    type="button"
                    onClick={() => setShowRefund(true)}
                    className="ml-auto px-6 py-2.5 text-xs font-bold uppercase tracking-widest"
                    style={{ color: C.redBright, border: `1px solid ${C.redDim}`, fontFamily: "Share Tech Mono" }}
                >
                  환불 신청
                </button>
            )}
          </div>
        </div>

        {showRefund && (
            <RefundModal
                paymentId={order.paymentId}
                onClose={() => setShowRefund(false)}
                onSuccess={refreshAfterRefund}
            />
        )}
      </>
  );
}

function OrderProgress({ status }: { status: OrderStatus }) {
  const current = STEPS.indexOf(status);

  return (
      <div className="relative grid grid-cols-4 mt-6">
        <div className="absolute top-[7px] left-[12.5%] right-[12.5%] h-px" style={{ background: C.panelBorder }} />

        {STEPS.map((step, i) => {
          const past = i < current;
          const now = i === current;

          return (
              <div key={step} className="relative flex flex-col items-center gap-2">
            <span
                className="w-3.5 h-3.5 rounded-full"
                style={{
                  background: past ? C.redBright : "#0e0000",
                  border: now ? `3px solid ${C.redBright}` : `1px solid ${past ? C.redBright : C.textMuted}`,
                }}
            />
                <span className="text-[11px]" style={{ color: past || now ? C.text : C.textMuted, fontWeight: now ? 700 : 400 }}>
              {ORDER_STATUS_LABEL[step]}
            </span>
              </div>
          );
        })}
      </div>
  );
}

function SectionLabel({ children }: { children: ReactNode }) {
  return (
      <p className="mb-3 text-[11px]" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
        {children}
      </p>
  );
}

function InfoRow({ label, children }: { label: string; children: ReactNode }) {
  return (
      <div className="flex gap-4 py-1.5 text-xs">
      <span className="w-24 shrink-0" style={{ color: C.textMuted }}>
        {label}
      </span>
        <span className="min-w-0 break-words" style={{ color: C.text }}>
        {children}
      </span>
      </div>
  );
}

function AmountRow({ label, children }: { label: string; children: ReactNode }) {
  return (
      <div className="flex items-center justify-between py-1.5 text-xs">
        <span style={{ color: C.textMuted }}>{label}</span>
        <span style={{ color: C.text, fontFamily: "Share Tech Mono" }}>{children}</span>
      </div>
  );
}