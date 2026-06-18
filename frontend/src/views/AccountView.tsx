import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { api, ApiError } from "../api";
import type { Account, HistoryRow } from "../api";
import { money, dateTime, movementLabel, shortId } from "../format";
import { BalanceReplay } from "../BalanceReplay";
import { Modal } from "../ui";
import type { Notify } from "../ui";

type ModalKind = null | "deposit" | "withdraw" | "transfer";

export function AccountView({ accountId, notify, onBack }: { accountId: string; notify: Notify; onBack: () => void }) {
  const [account, setAccount] = useState<Account | null>(null);
  const [rows, setRows] = useState<HistoryRow[] | null>(null);
  const [modal, setModal] = useState<ModalKind>(null);
  const [asOf, setAsOf] = useState("");
  const [asOfBalance, setAsOfBalance] = useState<string | null>(null);

  async function load() {
    try {
      const [a, h] = await Promise.all([api.balance(accountId), api.history(accountId)]);
      setAccount(a);
      setRows(h);
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không tải được tài khoản.", "err");
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accountId]);

  const currency = account?.currency ?? "VND";

  async function checkAsOf() {
    if (!asOf) return;
    try {
      const iso = new Date(asOf).toISOString();
      const r = await api.balanceAsOf(accountId, iso);
      setAsOfBalance(money(r.balance, currency));
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không truy vấn được số dư thời điểm.", "err");
    }
  }

  return (
    <div className="stack">
      <button className="ghost" onClick={onBack}>
        ← Tài khoản
      </button>

      {!account || !rows ? (
        <div className="skeleton" style={{ height: 180 }} />
      ) : (
        <>
          <BalanceReplay rows={rows} currency={currency} />

          <div className="row" style={{ flexWrap: "wrap" }}>
            <button className="primary" onClick={() => setModal("deposit")}>
              Nạp tiền
            </button>
            <button onClick={() => setModal("withdraw")}>Rút tiền</button>
            <button onClick={() => setModal("transfer")}>Chuyển tiền</button>
          </div>

          <div className="card">
            <div className="eyebrow">Số dư theo thời điểm</div>
            <div className="row" style={{ marginTop: 10, flexWrap: "wrap" }}>
              <input type="datetime-local" value={asOf} onChange={(e) => setAsOf(e.target.value)} style={{ maxWidth: 240 }} />
              <button onClick={checkAsOf} disabled={!asOf}>
                Xem lại
              </button>
              {asOfBalance && <span className="num">{asOfBalance}</span>}
            </div>
          </div>

          <div className="card">
            <h2>Sao kê</h2>
            {rows.length === 0 ? (
              <p className="muted">Chưa có giao dịch. Nạp tiền để bắt đầu.</p>
            ) : (
              <table className="ledger">
                <thead>
                  <tr>
                    <th className="left">Thời gian</th>
                    <th className="left">Loại</th>
                    <th>Ghi nợ</th>
                    <th>Ghi có</th>
                    <th>Số dư sau</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r, i) => (
                    <tr key={r.txId + i}>
                      <td className="left num faint">{dateTime(r.occurredAt)}</td>
                      <td className="left">
                        {movementLabel(r.movementType)}
                        {r.counterparty && <span className="faint"> · {shortId(r.counterparty)}</span>}
                      </td>
                      <td className="num debit">{r.direction === "D" ? money(r.amount, currency) : ""}</td>
                      <td className="num credit">{r.direction === "C" ? money(r.amount, currency) : ""}</td>
                      <td className="num">{money(r.balanceAfter, currency)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      {modal && (
        <ActionModal
          kind={modal}
          accountId={accountId}
          notify={notify}
          onClose={() => setModal(null)}
          onDone={async () => {
            setModal(null);
            await load();
          }}
        />
      )}
    </div>
  );
}

function ActionModal({
  kind,
  accountId,
  notify,
  onClose,
  onDone,
}: {
  kind: Exclude<ModalKind, null>;
  accountId: string;
  notify: Notify;
  onClose: () => void;
  onDone: () => Promise<void>;
}) {
  const [amount, setAmount] = useState("");
  const [to, setTo] = useState("");
  const [busy, setBusy] = useState(false);
  const title = kind === "deposit" ? "Nạp tiền" : kind === "withdraw" ? "Rút tiền" : "Chuyển tiền";

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const amt = Number(amount);
    if (!(amt > 0)) return;
    setBusy(true);
    try {
      if (kind === "deposit") await api.deposit(accountId, amt);
      else if (kind === "withdraw") await api.withdraw(accountId, amt);
      else await api.transfer(accountId, to.trim(), amt);
      notify(kind === "deposit" ? "Đã nạp tiền." : kind === "withdraw" ? "Đã rút tiền." : "Đã chuyển tiền.");
      await onDone();
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Giao dịch thất bại.", "err");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title={title} onClose={onClose}>
      <form className="stack" onSubmit={submit}>
        {kind === "transfer" && (
          <div className="field">
            <label htmlFor="to">Tài khoản nhận</label>
            <input id="to" value={to} onChange={(e) => setTo(e.target.value)} required placeholder="ID tài khoản đích" />
          </div>
        )}
        <div className="field">
          <label htmlFor="amt">Số tiền</label>
          <input id="amt" type="number" min="1" step="1" value={amount} onChange={(e) => setAmount(e.target.value)} required />
        </div>
        <button className="primary" type="submit" disabled={busy}>
          {busy ? "Đang xử lý" : "Xác nhận"}
        </button>
      </form>
    </Modal>
  );
}
