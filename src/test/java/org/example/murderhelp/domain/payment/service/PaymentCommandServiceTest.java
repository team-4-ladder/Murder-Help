package org.example.murderhelp.domain.payment.service;

import org.example.murderhelp.domain.cart.service.CartService;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.service.MemberSpendingService;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.payment.entity.FailReason;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.entity.PaymentStatus;
import org.example.murderhelp.domain.payment.repository.dto.PaymentWithItems;
import org.example.murderhelp.domain.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long PRODUCT_ID = 50L;
    private static final Long PAYMENT_AMOUNT = 150_000L;

    @Mock
    private PaymentService paymentService;

    @Mock
    private CartService cartService;

    @Mock
    private MemberSpendingService memberSpendingService;

    @InjectMocks
    private PaymentCommandService paymentCommandService;

    private Member member;
    private Order order;
    private Payment payment;
    private Product product;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        member = mock(Member.class);

        lenient()
                .when(member.getId())
                .thenReturn(MEMBER_ID);

        order = mock(Order.class);

        lenient()
                .when(order.getId())
                .thenReturn(ORDER_ID);

        lenient()
                .when(order.getMember())
                .thenReturn(member);

        lenient()
                .when(order.getStatus())
                .thenReturn(OrderStatus.PENDING_PAYMENT);

        product = mock(Product.class);

        lenient()
                .when(product.getId())
                .thenReturn(PRODUCT_ID);

        orderItem = mock(OrderItem.class);

        lenient()
                .when(orderItem.getProduct())
                .thenReturn(product);

        lenient()
                .when(orderItem.getQuantity())
                .thenReturn(2);

        payment = mock(Payment.class);

        lenient()
                .when(payment.getOrder())
                .thenReturn(order);

        lenient()
                .when(payment.getAmount())
                .thenReturn(PAYMENT_AMOUNT);
    }

    @Nested
    @DisplayName("approvePaymentAndOrder")
    class ApprovePaymentAndOrder {

        @Test
        @DisplayName("결제를 완료 처리하고 주문을 PAID로 전이시킨다")
        void completesPaymentAndTransitsOrderToPaid() {
            // given
            when(payment.getStatus())
                    .thenReturn(PaymentStatus.PENDING);

            PaymentWithItems paymentWithItems =
                    new PaymentWithItems(
                            payment,
                            List.of(orderItem)
                    );

            when(
                    paymentService.findByOrderIdWithOrderForUpdate(
                            ORDER_ID
                    )
            ).thenReturn(paymentWithItems);

            // when
            paymentCommandService.approvePaymentAndOrder(
                    ORDER_ID
            );

            // then
            verify(paymentService)
                    .completePayment(payment);

            verify(order)
                    .transitTo(OrderStatus.PAID);

            verify(order, never())
                    .transitTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("결제 완료 후 주문 상품을 productId 기준으로 장바구니에서 정리한다")
        void deletesCartItemsByProductIdAfterApproval() {
            // given
            when(payment.getStatus())
                    .thenReturn(PaymentStatus.PENDING);

            PaymentWithItems paymentWithItems =
                    new PaymentWithItems(
                            payment,
                            List.of(orderItem)
                    );

            when(
                    paymentService.findByOrderIdWithOrderForUpdate(
                            ORDER_ID
                    )
            ).thenReturn(paymentWithItems);

            // when
            paymentCommandService.approvePaymentAndOrder(
                    ORDER_ID
            );

            // then
            verify(cartService)
                    .deleteItemsByProductIds(
                            MEMBER_ID,
                            List.of(PRODUCT_ID)
                    );
        }

        @Test
        @DisplayName("결제 완료 후 결제금액을 회원 누적 구매금액에 반영한다")
        void addsPaymentAmountToMemberSpending() {
            // given
            when(payment.getStatus())
                    .thenReturn(PaymentStatus.PENDING);

            PaymentWithItems paymentWithItems =
                    new PaymentWithItems(
                            payment,
                            List.of(orderItem)
                    );

            when(
                    paymentService.findByOrderIdWithOrderForUpdate(
                            ORDER_ID
                    )
            ).thenReturn(paymentWithItems);

            // when
            paymentCommandService.approvePaymentAndOrder(
                    ORDER_ID
            );

            // then
            verify(memberSpendingService)
                    .addPaymentAmount(
                            MEMBER_ID,
                            PAYMENT_AMOUNT
                    );
        }

        @Test
        @DisplayName("이미 COMPLETED 상태면 중복 처리하지 않고 즉시 응답만 반환한다")
        void isIdempotentWhenAlreadyCompleted() {
            // given
            when(payment.getStatus())
                    .thenReturn(PaymentStatus.COMPLETED);

            PaymentWithItems paymentWithItems =
                    new PaymentWithItems(
                            payment,
                            List.of(orderItem)
                    );

            when(
                    paymentService.findByOrderIdWithOrderForUpdate(
                            ORDER_ID
                    )
            ).thenReturn(paymentWithItems);

            // when
            paymentCommandService.approvePaymentAndOrder(
                    ORDER_ID
            );

            // then
            verify(paymentService, never())
                    .completePayment(any());

            verify(order, never())
                    .transitTo(any());

            verify(cartService, never())
                    .deleteItemsByProductIds(
                            anyLong(),
                            anyList()
                    );

            verify(memberSpendingService, never())
                    .addPaymentAmount(
                            anyLong(),
                            anyLong()
                    );
        }
    }

    @Nested
    @DisplayName("failPaymentAndOrder")
    class FailPaymentAndOrder {

        @Test
        @DisplayName("결제를 실패 처리하고 주문을 취소하며 재고를 복구한다")
        void failsPaymentCancelsOrderAndRestoresStock() {
            // given
            PaymentWithItems paymentWithItems =
                    new PaymentWithItems(
                            payment,
                            List.of(orderItem)
                    );

            when(
                    paymentService.findByOrderIdWithOrder(
                            ORDER_ID
                    )
            ).thenReturn(paymentWithItems);

            // when
            paymentCommandService.failPaymentAndOrder(
                    ORDER_ID,
                    FailReason.PG_DECLINED
            );

            // then
            verify(paymentService)
                    .failPayment(
                            payment,
                            FailReason.PG_DECLINED
                    );

            verify(order)
                    .transitTo(OrderStatus.CANCELED);

            verify(product)
                    .restoreStock(2);

            verify(memberSpendingService, never())
                    .addPaymentAmount(
                            anyLong(),
                            anyLong()
                    );
        }
    }

    @Nested
    @DisplayName("cancelPaymentAndOrder")
    class CancelPaymentAndOrder {

        @Test
        @DisplayName("결제를 취소 처리하고 주문을 취소하며 재고를 복구한다")
        void cancelsPaymentCancelsOrderAndRestoresStock() {
            // given
            PaymentWithItems paymentWithItems =
                    new PaymentWithItems(
                            payment,
                            List.of(orderItem)
                    );

            when(
                    paymentService.findByOrderIdWithOrder(
                            ORDER_ID
                    )
            ).thenReturn(paymentWithItems);

            // when
            paymentCommandService.cancelPaymentAndOrder(
                    ORDER_ID
            );

            // then
            verify(paymentService)
                    .cancelPayment(payment);

            verify(order)
                    .transitTo(OrderStatus.CANCELED);

            verify(product)
                    .restoreStock(2);

            verify(memberSpendingService, never())
                    .addPaymentAmount(
                            anyLong(),
                            anyLong()
                    );
        }
    }
}