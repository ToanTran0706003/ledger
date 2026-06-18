import { useEffect, useState } from "react";
import type { HistoryRow } from "./api";
import { money, movementLabel } from "./format";

/**
 * Signature element (doc 06): dựng lại số dư từ CHUỖI SỰ KIỆN — cho thấy bản chất
 * event sourcing. Replay từng posting từ cũ tới mới, số dư cộng dồn. Tôn trọng
 * prefers-reduced-motion (hiện ngay kết quả cuối, không hoạt họa).
 */
export function BalanceReplay({ rows, currency }: { rows: HistoryRow[]; currency: string }) {
  const ordered = [...rows].reverse(); // API trả mới nhất trước -> đảo để replay từ đầu
  const reduced = typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const [step, setStep] = useState(reduced ? ordered.length : 0);

  useEffect(() => {
    if (reduced) {
      setStep(ordered.length);
      return;
    }
    setStep(0);
    let i = 0;
    const timer = setInterval(() => {
      i += 1;
      setStep(i);
      if (i >= ordered.length) clearInterval(timer);
    }, 260);
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rows.length]);

  const running = ordered
    .slice(0, step)
    .reduce((acc, r) => acc + (r.direction === "C" ? r.amount : -r.amount), 0);

  return (
    <div className="card">
      <div className="eyebrow">Dựng lại số dư từ chuỗi sự kiện</div>
      <div className="balance-hero" style={{ marginTop: 10 }}>{money(running, currency)}</div>
      {ordered.length > 0 && (
        <div className="replay-flow" aria-hidden="true">
          {ordered.map((r, i) => (
            <div key={r.txId + i} className={"replay-row" + (i < step ? " shown" : "")}>
              <span className="muted">{movementLabel(r.movementType)}</span>
              <span className={r.direction === "C" ? "credit" : "debit"}>
                {r.direction === "C" ? "+" : "−"}
                {money(r.amount, currency)}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
