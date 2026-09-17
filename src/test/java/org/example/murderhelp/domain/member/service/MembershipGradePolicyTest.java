package org.example.murderhelp.domain.member.service;

import org.example.murderhelp.domain.member.entity.Grade;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MembershipGradePolicyTest {

    @ParameterizedTest(name = "누적 구매금액 {0}원은 {1} 등급이다")
    @CsvSource({
            "0, YELLOW",
            "99_999, YELLOW",
            "100_000, PURPLE",
            "499_999, PURPLE",
            "500_000, RED",
            "1_000_000, RED"
    })
    void resolve(long netSpentAmount, Grade expectedGrade) {
        // when
        Grade result = MembershipGradePolicy.resolve(netSpentAmount);

        // then
        assertThat(result).isEqualTo(expectedGrade);
    }
}