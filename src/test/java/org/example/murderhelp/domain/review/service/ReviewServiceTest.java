package org.example.murderhelp.domain.review.service;

import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.order.repository.OrderItemRepository;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.review.dto.ReviewCreateRequest;
import org.example.murderhelp.domain.review.dto.ReviewResponse;
import org.example.murderhelp.domain.review.dto.ReviewUpdateRequest;
import org.example.murderhelp.domain.review.entity.Review;
import org.example.murderhelp.domain.review.repository.ReviewRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Captor
    private ArgumentCaptor<Review> reviewCaptor;

    @Test
    @DisplayName("내가 작성한 리뷰 목록을 최신순으로 조회한다")
    void getMyReviews() {
        // given
        Review review = createReview(100L, 10L, 1L, 1L, 5, "좋은 상품입니다.");

        when(reviewRepository.findAllByMemberIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(review));

        // when
        List<ReviewResponse> result = reviewService.getMyReviews(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reviewId()).isEqualTo(100L);
        assertThat(result.get(0).content()).isEqualTo("좋은 상품입니다.");

        verify(reviewRepository).findAllByMemberIdOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("배송완료된 본인 주문상품의 리뷰를 작성한다")
    void createReview() {
        // given
        OrderItem orderItem = createOrderItem(10L, 1L, OrderStatus.DELIVERED);

        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(10L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 100L);
            return review;
        });

        ReviewCreateRequest request =
                new ReviewCreateRequest(10L, 5, "배송도 빠르고 상품도 좋습니다.");

        // when
        ReviewResponse result = reviewService.createReview(1L, request);

        // then
        assertThat(result.reviewId()).isEqualTo(100L);
        assertThat(result.orderItemId()).isEqualTo(10L);
        assertThat(result.productId()).isEqualTo(1L);
        assertThat(result.rating()).isEqualTo(5);
        assertThat(result.content()).isEqualTo("배송도 빠르고 상품도 좋습니다.");

        verify(reviewRepository).save(reviewCaptor.capture());

        Review savedReview = reviewCaptor.getValue();
        assertThat(savedReview.getOrderItemId()).isEqualTo(10L);
        assertThat(savedReview.getProductId()).isEqualTo(1L);
        assertThat(savedReview.getMemberId()).isEqualTo(1L);
        assertThat(savedReview.getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("다른 회원의 주문상품에는 리뷰를 작성할 수 없다")
    void createReviewFailsForAnotherMembersOrder() {
        // given
        OrderItem orderItem = createOrderItem(10L, 2L, OrderStatus.DELIVERED);

        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(orderItem));

        ReviewCreateRequest request =
                new ReviewCreateRequest(10L, 5, "권한 없는 리뷰 작성");

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("배송완료 이전 주문상품에는 리뷰를 작성할 수 없다")
    void createReviewFailsWhenOrderIsNotDelivered() {
        // given
        OrderItem orderItem = createOrderItem(10L, 1L, OrderStatus.SHIPPING);

        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(orderItem));

        ReviewCreateRequest request =
                new ReviewCreateRequest(10L, 5, "배송 중 리뷰 작성");

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 리뷰가 작성된 주문상품에는 중복 리뷰를 작성할 수 없다")
    void createReviewFailsWhenReviewAlreadyExists() {
        // given
        OrderItem orderItem = createOrderItem(10L, 1L, OrderStatus.DELIVERED);

        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(10L)).thenReturn(true);

        ReviewCreateRequest request =
                new ReviewCreateRequest(10L, 5, "중복 리뷰 작성");

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("본인이 작성한 리뷰를 수정한다")
    void updateReview() {
        // given
        Review review = createReview(100L, 10L, 1L, 1L, 3, "수정 전 내용");

        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        ReviewUpdateRequest request =
                new ReviewUpdateRequest(5, "수정 후 내용");

        // when
        ReviewResponse result = reviewService.updateReview(1L, 100L, request);

        // then
        assertThat(result.rating()).isEqualTo(5);
        assertThat(result.content()).isEqualTo("수정 후 내용");
    }

    @Test
    @DisplayName("다른 회원의 리뷰는 수정할 수 없다")
    void updateReviewFailsForAnotherMember() {
        // given
        Review review = createReview(100L, 10L, 1L, 2L, 3, "다른 회원 리뷰");

        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        ReviewUpdateRequest request =
                new ReviewUpdateRequest(5, "수정 시도");

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(1L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("본인이 작성한 리뷰를 삭제한다")
    void deleteReview() {
        // given
        Review review = createReview(100L, 10L, 1L, 1L, 5, "삭제할 리뷰");

        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        // when
        reviewService.deleteReview(1L, 100L);

        // then
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("다른 회원의 리뷰는 삭제할 수 없다")
    void deleteReviewFailsForAnotherMember() {
        // given
        Review review = createReview(100L, 10L, 1L, 2L, 5, "다른 회원 리뷰");

        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(reviewRepository, never()).delete(any());
    }

    private OrderItem createOrderItem(
            Long orderItemId,
            Long memberId,
            OrderStatus targetStatus
    ) {
        Member member = Member.builder()
                .email("member" + memberId + "@test.com")
                .password("password")
                .name("회원")
                .phone("010-0000-0000")
                .build();

        ReflectionTestUtils.setField(member, "id", memberId);

        Order order = Order.builder()
                .member(member)
                .orderNumber("ORD-TEST-001")
                .totalAmount(15_900L)
                .receiverName("홍길동")
                .receiverPhone("010-0000-0000")
                .deliveryAddress("서울시 강남구")
                .deliveryRequest("문 앞에 놓아주세요.")
                .build();

        if (targetStatus != OrderStatus.PENDING_PAYMENT) {
            order.transitTo(OrderStatus.PAID);
        }

        if (targetStatus == OrderStatus.PREPARING_DELIVERY
                || targetStatus == OrderStatus.SHIPPING
                || targetStatus == OrderStatus.DELIVERED) {
            order.transitTo(OrderStatus.PREPARING_DELIVERY);
        }

        if (targetStatus == OrderStatus.SHIPPING
                || targetStatus == OrderStatus.DELIVERED) {
            order.transitTo(OrderStatus.SHIPPING);
        }

        if (targetStatus == OrderStatus.DELIVERED) {
            order.transitTo(OrderStatus.DELIVERED);
        }

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("리뷰 테스트 상품");
        when(product.getPrice()).thenReturn(15_900L);

        OrderItem orderItem = new OrderItem(order, product, 1);
        ReflectionTestUtils.setField(orderItem, "id", orderItemId);

        return orderItem;
    }

    private Review createReview(
            Long reviewId,
            Long orderItemId,
            Long productId,
            Long memberId,
            Integer rating,
            String content
    ) {
        Review review = Review.builder()
                .orderItemId(orderItemId)
                .productId(productId)
                .memberId(memberId)
                .rating(rating)
                .content(content)
                .build();

        ReflectionTestUtils.setField(review, "id", reviewId);

        return review;
    }
}