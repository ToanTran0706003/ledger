import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { Account, HistoryRow, IntegrityReport } from "../api";
import { money, shortId, dateTime, movementLabel } from "../format";
import type { Notify } from "../ui";

type Activity = HistoryRow & { accountId: string };

export function Dashboard({ notify, onOpenAccount }: { notify: Notify; onOpenAccount: (id: string) => void }) {
  const [accounts, setAccounts] = useState<Account[] | null>(null);
  const [activity, setActivity] = useState<Activity[]>([]);
  const [integrity, setIntegrity] = useState<IntegrityReport | null>(null);
  const [busy, setBusy] = useState(false);

  async function load() {
    try {
      const accs = await api.myAccounts();
      setAccounts(accs);
      const hists = await Promise.all(
        accs.map((a) => api.history(a.accountId).then((rows) => rows.map((r) => ({ ...r, accountId: a.accountId })))),
      );
      setActivity(hists.flat().sort((a, b) => b.occurredAt.localeCompare(a.occurredAt)));
      try {
        setIntegrity(await api.integrity());
      } catch {
        /* không chặn dashboard nếu integrity lỗi */
      }
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không tải được dữ liệu.", "err");
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function openAccount() {
    setBusy(true);
    try {
      await api.openAccount();
      notify("Đã mở tài khoản.");
      await load();
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không mở được tài khoản.", "err");
    } finally {
      setBusy(false);
    }
  }

  if (accounts === null) {
    return (
      <div className="stack">
        <div className="skeleton" style={{ height: 90 }} />
        <div className="skeleton" style={{ height: 170 }} />
      </div>
    );
  }

  const currency = accounts[0]?.currency ?? "VND";
  const total = accounts.reduce((s, a) => s + a.balance, 0);

  return (
    <div className="stack">
      <div className="spread">
        <div>
          <div className="eyebrow">Tổng quan</div>
          <h1>Bảng điều khiển</h1>
        </div>
        <button className="primary" onClick={openAccount} disabled={busy}>
          Mở tài khoản
        </button>
      </div>

      <div className="stat-grid">
        <div className="card stat">
          <div className="label">Tổng số dư</div>
          <div className="value">{money(total, currency)}</div>
        </div>
        <div className="card stat">
          <div className="label">Tài khoản</div>
          <div className="value">{accounts.length}</div>
        </div>
        <div className="card stat">
          <div className="label">Giao dịch</div>
          <div className="value">{activity.length}</div>
        </div>
      </div>

      {integrity && (
        <div className="card spread">
          <div>
            <h2>Toàn vẹn sổ cái</h2>
            <p className="muted" style={{ margin: 0 }}>
              Tổng mọi số dư khớp lượng tiền phát hành. Sổ luôn cân.
            </p>
          </div>
          <span className={"badge " + (integrity.balanced ? "ok" : "err")}>
            <span className="dot" />
            {integrity.balanced ? "Sổ cân" : "Lệch sổ"}
          </span>
        </div>
      )}

      {accounts.length === 0 ? (
        <div className="card">
          <h2>Chưa có tài khoản</h2>
          <p className="muted">Mở một tài khoản để bắt đầu nạp, rút và chuyển tiền.</p>
        </div>
      ) : (
        <div className="grid-2">
          {accounts.map((a) => (
            <button key={a.accountId} className="card account-tile" onClick={() => onOpenAccount(a.accountId)}>
              <div className="spread">
                <span className="tag">{a.status}</span>
                <span className="id">{shortId(a.accountId)}</span>
              </div>
              <div className="balance-hero" style={{ fontSize: 26 }}>
                {money(a.balance, a.currency)}
              </div>
            </button>
          ))}
        </div>
      )}

      {activity.length > 0 && (
        <div className="card">
          <h2>Hoạt động gần đây</h2>
          <div className="feed">
            {activity.slice(0, 8).map((r, i) => (
              <div key={r.txId + i} className="feed-row">
                <div className="meta">
                  <span>
                    {movementLabel(r.movementType)} <span className="faint">· {shortId(r.accountId)}</span>
                  </span>
                  <span className="when">{dateTime(r.occurredAt)}</span>
                </div>
                <span className={"amt " + (r.direction === "C" ? "credit" : "debit")}>
                  {r.direction === "C" ? "+" : "−"}
                  {money(r.amount, currency)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
