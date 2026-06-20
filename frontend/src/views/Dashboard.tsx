import { useEffect, useState } from "react";
import { api, ApiError, CURRENCIES } from "../api";
import type { Account, HistoryRow, IntegrityReport } from "../api";
import { money, shortId, dateTime, movementLabel, accountTypeLabel } from "../format";
import { Modal } from "../ui";
import type { Notify } from "../ui";

type Activity = HistoryRow & { accountId: string; currency: string };

export function Dashboard({ notify, onOpenAccount }: { notify: Notify; onOpenAccount: (id: string) => void }) {
  const [accounts, setAccounts] = useState<Account[] | null>(null);
  const [activity, setActivity] = useState<Activity[]>([]);
  const [integrity, setIntegrity] = useState<IntegrityReport | null>(null);
  const [opening, setOpening] = useState(false);

  async function load() {
    try {
      const accs = await api.myAccounts();
      setAccounts(accs);
      const hists = await Promise.all(
        accs.map((a) =>
          api.history(a.accountId).then((rows) => rows.map((r) => ({ ...r, accountId: a.accountId, currency: a.currency }))),
        ),
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

  if (accounts === null) {
    return (
      <div className="stack">
        <div className="skeleton" style={{ height: 90 }} />
        <div className="skeleton" style={{ height: 170 }} />
      </div>
    );
  }

  // Tổng theo TỪNG tiền tệ — không cộng chéo các currency khác nhau.
  const totalsByCurrency = accounts.reduce<Record<string, number>>((m, a) => {
    m[a.currency] = (m[a.currency] ?? 0) + a.balance;
    return m;
  }, {});
  const currencyTotals = Object.entries(totalsByCurrency);
  const currency = accounts[0]?.currency ?? "VND";

  return (
    <div className="stack">
      <div className="spread">
        <div>
          <div className="eyebrow">Tổng quan</div>
          <h1>Bảng điều khiển</h1>
        </div>
        <button className="primary" onClick={() => setOpening(true)}>
          Mở tài khoản
        </button>
      </div>

      <div className="stat-grid">
        <div className="card stat">
          <div className="label">Tổng số dư</div>
          <div className="value" style={{ display: "flex", flexDirection: "column", gap: 2 }}>
            {currencyTotals.length === 0 ? (
              money(0, currency)
            ) : (
              currencyTotals.map(([ccy, val]) => <span key={ccy}>{money(val, ccy)}</span>)
            )}
          </div>
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
        <div className="grid-auto">
          {accounts.map((a) => (
            <button
              key={a.accountId}
              className={"card account-tile" + (a.status === "FROZEN" ? " tile-frozen" : "")}
              onClick={() => onOpenAccount(a.accountId)}
            >
              <div className="spread">
                <span className="tag">{accountTypeLabel(a.type)}</span>
                {a.status === "FROZEN" ? (
                  <span className="chip-frozen">
                    <span aria-hidden="true">❄</span> Đóng băng
                  </span>
                ) : (
                  <span className="id">{shortId(a.accountId)}</span>
                )}
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
                  {money(r.amount, r.currency)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {opening && (
        <OpenAccountModal
          notify={notify}
          onClose={() => setOpening(false)}
          onOpened={async () => {
            setOpening(false);
            await load();
          }}
        />
      )}
    </div>
  );
}

function OpenAccountModal({
  notify,
  onClose,
  onOpened,
}: {
  notify: Notify;
  onClose: () => void;
  onOpened: () => Promise<void>;
}) {
  const [type, setType] = useState("CUSTOMER");
  const [currency, setCurrency] = useState("VND");
  const [busy, setBusy] = useState(false);

  async function submit() {
    setBusy(true);
    try {
      await api.openAccount(type, currency);
      notify(type === "SAVINGS" ? "Đã mở tài khoản tiết kiệm." : "Đã mở tài khoản.");
      await onOpened();
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không mở được tài khoản.", "err");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Mở tài khoản" onClose={onClose}>
      <div className="stack">
        <div className="field">
          <label htmlFor="acctype">Loại tài khoản</label>
          <select id="acctype" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="CUSTOMER">Thanh toán</option>
            <option value="SAVINGS">Tiết kiệm (có lãi)</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="acccur">Tiền tệ</label>
          <select id="acccur" value={currency} onChange={(e) => setCurrency(e.target.value)}>
            {CURRENCIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>
        <button className="primary" onClick={submit} disabled={busy}>
          {busy ? "Đang mở" : "Mở tài khoản"}
        </button>
      </div>
    </Modal>
  );
}
