package org.example.murderhelp.domain.member.dto;

import org.example.murderhelp.domain.member.entity.MemberSpending;

public record MemberSpendingResponse(
        Long netSpentAmount,
        String grade
) {

    public static MemberSpendingResponse from(MemberSpending memberSpending) {
        return new MemberSpendingResponse(
                memberSpending.getNetSpentAmount(),
                memberSpending.getResolvedGrade().name()
        );
    }
}