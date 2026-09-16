import type { Member } from "../api/auth";
import type { Tier } from "../catalog";
import { C } from "./theme";

/* ─── tier system ───────────────────────────────────────── */
export const TIERS: Record<
    Tier,
    {
      label: string;
      color: string;
      brightColor: string;
      desc: string;
      priceRange: string;
    }
> = {
  green: {
    label: "Code Green",
    color: "#10b981",
    brightColor: "#34d399",
    desc: "본사 관리자 전용 / HQ Administration",
    priceRange: "Admin",
  },
  red: {
    label: "Code Red",
    color: C.red,
    brightColor: C.redBright,
    desc: "VIP 회원 등급 — 프리미엄 한정판 장비",
    priceRange: "3,000원~",
  },
  purple: {
    label: "Code Purple",
    color: C.purple,
    brightColor: C.purpleBright,
    desc: "중급 회원 등급 — 중급 퍼포먼스 장비",
    priceRange: "1,000 ~ 3,000원",
  },
  yellow: {
    label: "Code Yellow",
    color: C.yellow,
    brightColor: C.yellowBright,
    desc: "기본 회원 등급 — 엔트리급 BB탄 장비",
    priceRange: "~1,000원",
  },
};

/* 서버 MembershipGradePolicy 기준과 동일하게 유지 */
export const PURPLE_AT = 100_000;
export const RED_AT = 500_000;

export function tierFor(
    spent: number,
    id?: string,
): Tier {
  if (id === "green") {
    return "green";
  }

  if (spent >= RED_AT) {
    return "red";
  }

  if (spent >= PURPLE_AT) {
    return "purple";
  }

  return "yellow";
}

/*
 * LoginModal의 기존 콜백 계약을 유지하기 위한 임시 변환 함수.
 * 실제 누적 구매금액은 App.tsx에서 /api/members/me/spending을
 * 호출한 결과로 교체한다.
 */
export function spentFromGrade(
    grade: Member["grade"],
) {
  switch (grade) {
    case "GREEN":
      return 1_000_000;
    case "RED":
      return RED_AT;
    case "PURPLE":
      return PURPLE_AT;
    case "YELLOW":
    default:
      return 0;
  }
}

export function nextTier(
    spent: number,
    id?: string,
): {
  tier: Tier;
  remaining: number;
} | null {
  if (id === "green") {
    return null;
  }

  if (spent >= RED_AT) {
    return null;
  }

  if (spent >= PURPLE_AT) {
    return {
      tier: "red",
      remaining: RED_AT - spent,
    };
  }

  return {
    tier: "purple",
    remaining: PURPLE_AT - spent,
  };
}

export const TIER_RANK: Record<Tier, number> = {
  yellow: 0,
  purple: 1,
  red: 2,
  green: 99,
};

export function canAccess(
    userTier: Tier | null,
    productTier: Tier,
) {
  if (!userTier) {
    return false;
  }

  return (
      TIER_RANK[productTier] <=
      TIER_RANK[userTier]
  );
}