package org.example.murderhelp.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.member.dto.MemberSpendingResponse;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.entity.MemberSpending;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.domain.member.repository.MemberSpendingRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSpendingService {

    private final MemberSpendingRepository memberSpendingRepository;
    private final MemberRepository memberRepository;

    /**
     * 로그인 회원의 누적 구매금액 및 현재 등급 조회
     */
    public MemberSpendingResponse getMySpending(Long memberId) {
        MemberSpending memberSpending =
                memberSpendingRepository.findByMemberId(memberId)
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
                        );

        return MemberSpendingResponse.from(memberSpending);
    }

    /**
     * 결제 완료 시 누적 구매금액 증가 및 회원 등급 갱신
     */
    @Transactional
    public void addPaymentAmount(Long memberId, long amount) {
        if (amount <= 0) {
            return;
        }

        MemberSpending memberSpending =
                getMemberSpendingForUpdate(memberId);

        Member member = getMember(memberId);

        memberSpending.addAmount(amount);
        member.changeGrade(memberSpending.getResolvedGrade());
    }

    /**
     * 환불 확정 시 누적 구매금액 감소 및 회원 등급 갱신
     */
    @Transactional
    public void subtractRefundAmount(Long memberId, long amount) {
        if (amount <= 0) {
            return;
        }

        MemberSpending memberSpending =
                getMemberSpendingForUpdate(memberId);

        Member member = getMember(memberId);

        memberSpending.subtractAmount(amount);
        member.changeGrade(memberSpending.getResolvedGrade());
    }

    private MemberSpending getMemberSpendingForUpdate(Long memberId) {
        return memberSpendingRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
                );
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
                );
    }
}