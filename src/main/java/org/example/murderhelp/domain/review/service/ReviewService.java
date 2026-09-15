package org.example.murderhelp.domain.review.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.order.repository.OrderItemRepository;
import org.example.murderhelp.domain.review.dto.*;
import org.example.murderhelp.domain.review.entity.Review;
import org.example.murderhelp.domain.review.repository.ReviewQueryRepository;
import org.example.murderhelp.domain.review.repository.ReviewRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewQueryRepository reviewQueryRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 리뷰 작성 가능한 주문상품 조회
     *
     * 배송 완료 상태이며 아직 리뷰가 작성되지 않은
     * 로그인 회원의 주문상품만 반환한다.
     */
    public List<PendingReviewResponse> getPendingReviews(Long memberId) {
        return reviewQueryRepository.findPendingReviewItems(memberId)
                .stream()
                .map(PendingReviewResponse::from)
                .toList();
    }

    /**
     * 로그인 회원이 작성한 리뷰 조회
     */
    public List<MyReviewResponse> getMyReviews(Long memberId) {
        return reviewQueryRepository.findMyReviews(memberId);
    }

    /**
     * 특정 상품에 등록된 리뷰를 최신순으로 조회한다.
     */
    public List<ReviewResponse> getProductReviews(Long productId) {
        return reviewRepository
                .findAllByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * 리뷰 작성
     */
    @Transactional
    public ReviewResponse createReview(
            Long memberId,
            ReviewCreateRequest request
    ) {
        OrderItem orderItem = orderItemRepository.findById(request.orderItemId())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.ORDER_NOT_FOUND)
                );

        // 로그인한 회원이 주문한 상품인지 확인
        if (!orderItem.getOrder().getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 배송 완료된 주문만 리뷰 작성 가능
        if (orderItem.getOrder().getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "배송 완료된 상품만 리뷰를 작성할 수 있습니다."
            );
        }

        // 주문상품당 리뷰는 한 번만 작성 가능
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 작성한 리뷰가 있습니다."
            );
        }

        Review review = Review.builder()
                .orderItemId(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .memberId(memberId)
                .rating(request.rating())
                .content(request.content())
                .build();

        Review savedReview = reviewRepository.save(review);

        return ReviewResponse.from(savedReview);
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public ReviewResponse updateReview(
            Long memberId,
            Long reviewId,
            ReviewUpdateRequest request
    ) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.REVIEW_NOT_FOUND)
                );

        // 본인이 작성한 리뷰인지 확인
        if (!review.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.update(
                request.rating(),
                request.content()
        );

        return ReviewResponse.from(review);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(
            Long memberId,
            Long reviewId
    ) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.REVIEW_NOT_FOUND)
                );

        // 본인이 작성한 리뷰인지 확인
        if (!review.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        reviewRepository.delete(review);
    }
}