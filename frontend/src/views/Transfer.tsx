import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { api, ApiError } from "../api";
import type { Account } from "../api";
import { money, shortId } from "../format";
import type { Notify } from "../ui";

export function Transfer({ notify, onDone }: { notify: Notify; onDone: () => void }) {
  const [accounts, setAccounts] = useState<Account[] | null>(null);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [amount, setAmount] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .myAccounts()
      .then((a) => {
        setAccounts(a);
        if (a[0]) setFrom(a[0].accountId);
      })
      .catch((ex) => notify(ex instanceof ApiError ? ex.message : "Không tải được tài khoản.", "err"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const amt = Number(amount);
    if (!(amt > 0) || !from || !to.trim()) return;
    setBusy(true);
    try {
      const r = await api.transfer(from, to.trim(), amt);
      notify(r.status === "PENDING_APPROVAL" ? "Vượt ngưỡng — đã gửi yêu cầu, chờ ADMIN duyệt." : "Đã chuyển tiền.");
      onDone();
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Chuyển tiền thất bại.", "err");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack" style={{ maxWidth: 520, marginInline: "auto", width: "100%" }}>
      <div>
        <div className="eyebrow">Lệnh</div>
        <h1>Chuyển tiền</h1>
      </div>
      <form className="card stack" onSubmit={submit}>
        <div className="field">
          <label htmlFor="from">Từ tài khoản</label>
          <select id="from" value={from} onChange={(e) => setFrom(e.target.value)} required>
            {(accounts ?? []).map((a) => (
              <option key={a.accountId} value={a.accountId}>
                {shortId(a.accountId)} · {money(a.balance, a.currency)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="to">Đến tài khoản (ID)</label>
          <input id="to" value={to} onChange={(e) => setTo(e.target.value)} required placeholder="Dán ID tài khoản đích" />
        </div>
        <div className="field">
          <label htmlFor="amt">Số tiền</label>
          <input id="amt" type="number" min="1" step="1" value={amount} onChange={(e) => setAmount(e.target.value)} required />
        </div>
        <button className="primary" type="submit" disabled={busy}>
          {busy ? "Đang chuyển" : "Chuyển tiền"}
        </button>
      </form>
      <p className="muted">
        Chỉ chuyển được từ tài khoản của bạn. Lệnh khử trùng lặp (Idempotency-Key) và ghi sổ kép cân vế: trừ ở nguồn,
        cộng ở đích cùng một giao dịch.
      </p>
    </div>
  );
}
