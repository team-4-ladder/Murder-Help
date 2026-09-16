import type { Tier } from "../../catalog";
import { TIERS } from "../../lib/tier";

export function TierBadge({ tier, small, compact }: { tier: Tier; small?: boolean; compact?: boolean }) {
  const t = TIERS[tier];
  return (
    <span
      className={`uppercase font-bold tracking-wider rounded-full flex items-center justify-center shrink-0 whitespace-nowrap ${
        compact ? "text-[10px] px-1.5 py-0.5" : ""
      }`}
      style={{
        fontFamily: "Share Tech Mono, sans-serif",
        ...(compact ? {} : { fontSize: small ? 9 : 10, padding: small ? "2px 6px" : "3px 8px" }),
        color: t.brightColor,
        background: `${t.color}25`,
        border: `1px solid ${t.color}40`,
        lineHeight: 1,
      }}
    >
      {compact ? tier.toUpperCase() : t.label}
    </span>
  );
}
