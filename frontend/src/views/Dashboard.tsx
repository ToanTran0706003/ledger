import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { Account } from "../api";
import { money, shortId } from "../format";
import type { Notify } from "../ui";

export function Dashboard({ notify, onOpenAccount }: { notify: Notify; onOpenAccount: (id: string) => void }) {
  const [accounts, setAccounts] = useState<Account[] | null>(null);
  const [busy, setBusy] = useState(false);

  async function load() {
    try {
      setAccounts(await api.myAccounts());
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không tải được danh sách tài khoản.", "err");
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

  return (
    <div className="stack">
      <div className="spread">
        <div>
          <div className="eyebrow">Tổng quan</div>
          <h1>Tài khoản của bạn</h1>
        </div>
        <button className="primary" onClick={openAccount} disabled={busy}>
          Mở tài khoản
        </button>
      </div>

      {accounts === null ? (
        <div className="stack">
          {[0, 1].map((i) => (
            <div key={i} className="skeleton" style={{ height: 88 }} />
          ))}
        </div>
      ) : accounts.length === 0 ? (
        <div className="card">
          <h2>Chưa có tài khoản</h2>
          <p className="muted">Mở một tài khoản để bắt đầu nạp, rút và chuyển tiền.</p>
        </div>
      ) : (
        <div className="stack">
          {accounts.map((a) => (
            <button key={a.accountId} className="card account-tile" onClick={() => onOpenAccount(a.accountId)}>
              <div className="spread">
                <span className="tag">{a.status}</span>
                <span className="id">{shortId(a.accountId)}</span>
              </div>
              <div className="balance-hero" style={{ fontSize: 28 }}>
                {money(a.balance, a.currency)}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
