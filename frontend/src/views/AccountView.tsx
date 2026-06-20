import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { api, ApiError } from "../api";
import type { Account, HistoryRow, HoldView } from "../api";
import { money, dateTime, movementLabel, holdStatusLabel, shortId } from "../format";
import { BalanceReplay } from "../BalanceReplay";
import { AreaChart } from "../chart";
import { Modal } from "../ui";
import type { Notify } from "../ui";

type ModalKind = null | "deposit" | "withdraw" | "transfer" | "hold" | "exchange";

export function AccountView({ accountId, notify, onBack }: { accountId: string; notify: Notify; onBack: () => void }) {
  const [account, setAccount] = useState<Account | null>(null);
  const [rows, setRows] = useState<HistoryRow[] | null>(null);
  const [holds, setHolds] = useState<HoldView[]>([]);
  const [modal, setModal] = useState<ModalKind>(null);
  const [step, setStep] = useState(0);

  async function load() {
    try {
      const [a, h, hd] = await Promise.all([
        api.balance(accountId),
        api.history(accountId),
        api.holds(accountId),
      ]);
      setAccount(a);
      setRows(h);
      setHolds(hd);
      setStep(h.length); // mặc định ở hiện tại
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không tải được tài khoản.", "err");
    }
  }

  async function holdAction(fn: () => Promise<unknown>, okMsg: string) {
    try {
      await fn();
      notify(okMsg);
      await load();
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Thao tác thất bại.", "err");
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accountId]);

  const currency = account?.currency ?? "VND";
  const ordered = rows ? [...rows].reverse() : []; // cũ -> mới
  const series = [0, ...ordered.map((r) => r.balanceAfter)]; // số dư theo thời gian, bắt đầu từ 0
  const atStep = series[Math.min(step, series.length - 1)] ?? 0;
  const stepTime = step > 0 && ordered[step - 1] ? ordered[step - 1].occurredAt : null;

  async function verifyAsOf() {
    if (!stepTime) return;
    try {
      const r = await api.balanceAsOf(accountId, new Date(stepTime).toISOString());
      const match = r.balance === atStep;
      notify(
        match
          ? `Máy chủ xác nhận: ${money(r.balance, currency)} tại thời điểm đó.`
          : `Lệch: máy chủ trả ${money(r.balance, currency)}.`,
        match ? "ok" : "err",
      );
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không truy vấn được.", "err");
    }
  }

  return (
    <div className="stack">
      <button className="ghost" onClick={onBack}>
        ← Bảng điều khiển
      </button>

      {!account || !rows ? (
        <div className="skeleton" style={{ height: 200 }} />
      ) : (
        <>
          <BalanceReplay rows={rows} currency={currency} />

          {account.status === "FROZEN" && (
            <div className="frozen-banner" role="status">
              <div className="mark" aria-hidden="true">❄</div>
              <div>
                <div className="title">Tài khoản đang bị đóng băng</div>
                {account.freezeReason && <div className="reason">{account.freezeReason}</div>}
                <div className="note">Tiền chỉ có thể nạp vào. Rút và chuyển tạm khoá cho tới khi mở băng.</div>
              </div>
            </div>
          )}

          <div className="row" style={{ flexWrap: "wrap" }}>
            <button className="primary" onClick={() => setModal("deposit")}>
              Nạp tiền
            </button>
            <button onClick={() => setModal("withdraw")} disabled={account.status === "FROZEN"}>
              Rút tiền
            </button>
            <button onClick={() => setModal("transfer")} disabled={account.status === "FROZEN"}>
              Chuyển tiền
            </button>
            <button onClick={() => setModal("hold")} disabled={account.status === "FROZEN"}>
              Đặt giữ
            </button>
            <button onClick={() => setModal("exchange")} disabled={account.status === "FROZEN"}>
              Quy đổi
            </button>
            <span className="faint num" style={{ marginLeft: "auto", alignSelf: "center" }}>{shortId(accountId)}</span>
          </div>

          {account.balance - account.available > 0 && (
            <div className="card spread">
              <div>
                <div className="eyebrow">Khả dụng</div>
                <div className="num" style={{ fontSize: 20 }}>{money(account.available, currency)}</div>
              </div>
              <div style={{ textAlign: "right" }}>
                <div className="eyebrow">Đang giữ</div>
                <div className="num debit" style={{ fontSize: 20 }}>{money(account.balance - account.available, currency)}</div>
              </div>
            </div>
          )}

          <div className="card">
            <div className="spread">
              <h2>Số dư theo thời gian</h2>
              <span className="tag">Time travel</span>
            </div>
            <AreaChart values={series} highlight={step} />
            <div className="row" style={{ marginTop: 12, flexWrap: "wrap" }}>
              <input
                type="range"
                min={0}
                max={rows.length}
                value={step}
                onChange={(e) => setStep(Number(e.target.value))}
                aria-label="Tua thời điểm"
                style={{ flex: 1, minWidth: 180 }}
              />
              <span className="num">{money(atStep, currency)}</span>
            </div>
            <div className="spread" style={{ marginTop: 8 }}>
              <span className="faint num">{stepTime ? dateTime(stepTime) : "Khởi điểm"}</span>
              <button className="ghost" onClick={verifyAsOf} disabled={!stepTime}>
                Kiểm chứng từ máy chủ
              </button>
            </div>
          </div>

          {holds.length > 0 && (
            <div className="card">
              <div className="spread">
                <h2>Tiền đang giữ</h2>
                <span className="tag">Hold</span>
              </div>
              <div className="feed">
                {holds.map((h) => {
                  const active = h.status === "ACTIVE";
                  return (
                    <div key={h.holdId} className="feed-row">
                      <div className="meta">
                        <span>
                          {money(h.amount, currency)}{" "}
                          <span className={"badge " + (active ? "" : "ok")} style={{ marginLeft: 6 }}>
                            {holdStatusLabel(h.status)}
                          </span>
                        </span>
                        <span className="when">
                          {active ? `Hết hạn ${dateTime(h.expiresAt)}` : `Đặt ${dateTime(h.placedAt)}`}
                        </span>
                      </div>
                      {active && (
                        <div className="row" style={{ gap: 8 }}>
                          <button
                            className="ghost"
                            onClick={() => holdAction(() => api.releaseHold(accountId, h.holdId), "Đã nhả khoản giữ.")}
                          >
                            Nhả
                          </button>
                          <button
                            className="primary"
                            onClick={() => holdAction(() => api.captureHold(accountId, h.holdId), "Đã thu khoản giữ.")}
                          >
                            Thu
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}

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

      {modal === "exchange" && account ? (
        <ExchangeModal
          accountId={accountId}
          fromCurrency={account.currency}
          notify={notify}
          onClose={() => setModal(null)}
          onDone={async () => {
            setModal(null);
            await load();
          }}
        />
      ) : modal ? (
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
      ) : null}
    </div>
  );
}

function ExchangeModal({
  accountId,
  fromCurrency,
  notify,
  onClose,
  onDone,
}: {
  accountId: string;
  fromCurrency: string;
  notify: Notify;
  onClose: () => void;
  onDone: () => Promise<void>;
}) {
  const [targets, setTargets] = useState<Account[] | null>(null);
  const [to, setTo] = useState("");
  const [amount, setAmount] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    // Đích quy đổi: tài khoản khác của mình có TIỀN TỆ KHÁC.
    api
      .myAccounts()
      .then((accs) => {
        const eligible = accs.filter((a) => a.accountId !== accountId && a.currency !== fromCurrency);
        setTargets(eligible);
        setTo(eligible[0]?.accountId ?? "");
      })
      .catch((ex) => notify(ex instanceof ApiError ? ex.message : "Không tải được tài khoản.", "err"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const amt = Number(amount);
    if (!(amt > 0) || !to) return;
    setBusy(true);
    try {
      await api.exchange(accountId, to, amt);
      notify("Đã quy đổi tiền tệ.");
      await onDone();
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Quy đổi thất bại.", "err");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Quy đổi tiền tệ" onClose={onClose}>
      {targets === null ? (
        <div className="skeleton" style={{ height: 90 }} />
      ) : targets.length === 0 ? (
        <p className="muted">
          Chưa có tài khoản tiền tệ khác để quy đổi. Mở một tài khoản tiền tệ khác trước.
        </p>
      ) : (
        <form className="stack" onSubmit={submit}>
          <div className="field">
            <label htmlFor="fx-to">Đến tài khoản (tiền tệ khác)</label>
            <select id="fx-to" value={to} onChange={(e) => setTo(e.target.value)} required>
              {targets.map((a) => (
                <option key={a.accountId} value={a.accountId}>
                  {a.currency} · {shortId(a.accountId)}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="fx-amt">Số tiền ({fromCurrency})</label>
            <input id="fx-amt" type="number" min="1" step="1" value={amount} onChange={(e) => setAmount(e.target.value)} required />
          </div>
          <p className="faint" style={{ fontSize: 13, margin: 0 }}>Tỉ giá do hệ thống quy định.</p>
          <button className="primary" type="submit" disabled={busy}>
            {busy ? "Đang quy đổi" : "Quy đổi"}
          </button>
        </form>
      )}
    </Modal>
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
  const [ttl, setTtl] = useState("3600");
  const [busy, setBusy] = useState(false);
  const title =
    kind === "deposit"
      ? "Nạp tiền"
      : kind === "withdraw"
        ? "Rút tiền"
        : kind === "transfer"
          ? "Chuyển tiền"
          : "Đặt giữ tiền";

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const amt = Number(amount);
    if (!(amt > 0)) return;
    setBusy(true);
    try {
      if (kind === "deposit") await api.deposit(accountId, amt);
      else if (kind === "withdraw") await api.withdraw(accountId, amt);
      else if (kind === "transfer") await api.transfer(accountId, to.trim(), amt);
      else await api.placeHold(accountId, amt, Number(ttl));
      notify(
        kind === "deposit"
          ? "Đã nạp tiền."
          : kind === "withdraw"
            ? "Đã rút tiền."
            : kind === "transfer"
              ? "Đã chuyển tiền."
              : "Đã đặt giữ tiền.",
      );
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
        {kind === "hold" && (
          <div className="field">
            <label htmlFor="ttl">Thời hạn giữ</label>
            <select id="ttl" value={ttl} onChange={(e) => setTtl(e.target.value)}>
              <option value="30">30 giây (demo)</option>
              <option value="3600">1 giờ</option>
              <option value="86400">1 ngày</option>
            </select>
          </div>
        )}
        <button className="primary" type="submit" disabled={busy}>
          {busy ? "Đang xử lý" : "Xác nhận"}
        </button>
      </form>
    </Modal>
  );
}
