import { useEffect, useState } from "react";
import type { HistoryRow } from "./api";
import { money, movementLabel } from "./format";

const MAX_REPLAY = 6; // chỉ diễn hoạt ~6 sự kiện gần nhất để không quá dài khi lịch sử lớn

/**
 * Signature element (doc 06): dựng lại số dư từ CHUỖI SỰ KIỆN — cho thấy bản chất event
 * sourcing. Khi lịch sử dài, gộp phần cũ thành "+N giao dịch trước đó" (= số dư nền) rồi diễn
 * hoạt các posting gần nhất cộng dồn lên. Tôn trọng prefers-reduced-motion (hiện ngay kết quả).
 */
export function BalanceReplay({ rows, currency }: { rows: HistoryRow[]; currency: string }) {
  const ordered = [...rows].reverse(); // API trả mới nhất trước -> đảo để replay từ cũ tới mới
  const hidden = Math.max(0, ordered.length - MAX_REPLAY);
  const shown = ordered.slice(hidden); // các posting gần nhất sẽ diễn hoạt
  const delta = (r: HistoryRow) => (r.direction === "C" ? r.amount : -r.amount);
  const base = ordered.slice(0, hidden).reduce((acc, r) => acc + delta(r), 0); // số dư trước cửa sổ

  const reduced = typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const [step, setStep] = useState(reduced ? shown.length : 0);

  useEffect(() => {
    if (reduced) {
      setStep(shown.length);
      return;
    }
    setStep(0);
    let i = 0;
    const timer = setInterval(() => {
      i += 1;
      setStep(i);
      if (i >= shown.length) clearInterval(timer);
    }, 260);
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rows.length]);

  const running = base + shown.slice(0, step).reduce((acc, r) => acc + delta(r), 0);

  return (
    <div className="card">
      <div className="eyebrow">Dựng lại số dư từ chuỗi sự kiện</div>
      <div className="balance-hero" style={{ marginTop: 10 }}>{money(running, currency)}</div>
      {shown.length > 0 && (
        <div className="replay-flow" aria-hidden="true">
          {hidden > 0 && (
            <div className="replay-row shown faint">
              <span>+ {hidden} giao dịch trước đó</span>
              <span className="num">{money(base, currency)}</span>
            </div>
          )}
          {shown.map((r, i) => (
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
