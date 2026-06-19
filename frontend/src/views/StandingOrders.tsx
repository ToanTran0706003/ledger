import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { api, ApiError } from "../api";
import type { Account, StandingOrderView } from "../api";
import { money, shortId, dateTime, intervalLabel } from "../format";
import type { Notify } from "../ui";

export function StandingOrders({ notify }: { notify: Notify }) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [orders, setOrders] = useState<StandingOrderView[] | null>(null);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [amount, setAmount] = useState("");
  const [intervalSeconds, setIntervalSeconds] = useState("86400");
  const [busy, setBusy] = useState(false);

  async function load() {
    try {
      const [accs, ords] = await Promise.all([api.myAccounts(), api.standingOrders()]);
      setAccounts(accs);
      setFrom((cur) => cur || (accs[0]?.accountId ?? ""));
      setOrders(ords);
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không tải được dữ liệu.", "err");
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const amt = Number(amount);
    if (!(amt > 0) || !from || !to.trim()) return;
    setBusy(true);
    try {
      await api.createStandingOrder(from, to.trim(), amt, Number(intervalSeconds));
      notify("Đã tạo lệnh định kỳ.");
      setTo("");
      setAmount("");
      await load();
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không tạo được lệnh.", "err");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack" style={{ maxWidth: 680 }}>
      <div>
        <div className="eyebrow">Tự động</div>
        <h1>Chuyển tiền định kỳ</h1>
      </div>

      <form className="card stack" onSubmit={submit}>
        <div className="field">
          <label htmlFor="so-from">Từ tài khoản</label>
          <select id="so-from" value={from} onChange={(e) => setFrom(e.target.value)} required>
            {accounts.map((a) => (
              <option key={a.accountId} value={a.accountId}>
                {shortId(a.accountId)} · {money(a.balance, a.currency)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="so-to">Đến tài khoản (ID)</label>
          <input id="so-to" value={to} onChange={(e) => setTo(e.target.value)} required placeholder="Dán ID tài khoản đích" />
        </div>
        <div className="row" style={{ gap: 12, alignItems: "flex-end" }}>
          <div className="field" style={{ flex: 1, marginBottom: 0 }}>
            <label htmlFor="so-amt">Số tiền</label>
            <input id="so-amt" type="number" min="1" step="1" value={amount} onChange={(e) => setAmount(e.target.value)} required />
          </div>
          <div className="field" style={{ flex: 1, marginBottom: 0 }}>
            <label htmlFor="so-int">Chu kỳ</label>
            <select id="so-int" value={intervalSeconds} onChange={(e) => setIntervalSeconds(e.target.value)}>
              <option value="30">mỗi 30 giây (demo)</option>
              <option value="3600">mỗi giờ</option>
              <option value="86400">mỗi ngày</option>
              <option value="604800">mỗi tuần</option>
            </select>
          </div>
        </div>
        <button className="primary" type="submit" disabled={busy}>
          {busy ? "Đang tạo" : "Tạo lệnh"}
        </button>
      </form>

      <div className="card">
        <h2>Lệnh của bạn</h2>
        {orders === null ? (
          <div className="skeleton" style={{ height: 60 }} />
        ) : orders.length === 0 ? (
          <p className="muted">Chưa có lệnh định kỳ nào.</p>
        ) : (
          <div className="feed">
            {orders.map((o) => (
              <div key={o.id} className="feed-row">
                <div className="meta">
                  <span>
                    {shortId(o.fromAccountId)} → {shortId(o.toAccountId)}{" "}
                    <span className="faint">· {intervalLabel(o.intervalSeconds)}</span>
                  </span>
                  <span className="when">Kỳ kế: {dateTime(o.nextRunAt)}</span>
                </div>
                <span className="amt">{money(o.amount)}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      <p className="muted">
        Scheduler tự chạy lệnh đến hạn (at-most-once). Tạo lệnh "mỗi 30 giây" rồi quay lại Tổng quan sau ít giây để
        thấy tiền tự chuyển.
      </p>
    </div>
  );
}
