import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { TwoFactorSetup } from "../api";
import type { Notify } from "../ui";

const onlyDigits = (v: string) => v.replace(/\D/g, "").slice(0, 6);

/** Quản lý xác thực hai lớp (TOTP): bật (ghi danh + xác nhận mã) và tắt (cần mã hợp lệ). */
export function Security({ notify }: { notify: Notify }) {
  const [enabled, setEnabled] = useState<boolean | null>(null);
  const [setup, setSetup] = useState<TwoFactorSetup | null>(null);
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .twoFactorStatus()
      .then((s) => setEnabled(s.enabled))
      .catch(() => setEnabled(false));
  }, []);

  const fail = (ex: unknown) => notify(ex instanceof ApiError ? ex.message : "Không kết nối được máy chủ.", "err");

  async function startSetup() {
    setBusy(true);
    try {
      setSetup(await api.setup2fa());
      setCode("");
    } catch (ex) {
      fail(ex);
    } finally {
      setBusy(false);
    }
  }

  async function confirm() {
    setBusy(true);
    try {
      await api.enable2fa(code);
      notify("Đã bật xác thực hai lớp.");
      setEnabled(true);
      setSetup(null);
      setCode("");
    } catch (ex) {
      fail(ex);
    } finally {
      setBusy(false);
    }
  }

  async function disable() {
    setBusy(true);
    try {
      await api.disable2fa(code);
      notify("Đã tắt xác thực hai lớp.");
      setEnabled(false);
      setCode("");
    } catch (ex) {
      fail(ex);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack" style={{ maxWidth: 560, marginInline: "auto", width: "100%" }}>
      <div>
        <div className="eyebrow">Bảo mật</div>
        <h1>Xác thực hai lớp</h1>
      </div>
      <p className="muted">
        Thêm một lớp bảo vệ: khi đăng nhập cần thêm mã 6 số đổi mỗi 30 giây từ ứng dụng xác thực
        (Google Authenticator, Authy…).
      </p>

      {enabled === null ? (
        <div className="skeleton" style={{ height: 140 }} />
      ) : enabled ? (
        <div className="card stack">
          <span className="badge ok" style={{ justifySelf: "start" }}>
            <span className="dot" /> Đã bật
          </span>
          <p className="muted">Nhập mã hiện tại trong app để tắt.</p>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="dc">Mã xác thực</label>
            <input id="dc" className="num" value={code} onChange={(e) => setCode(onlyDigits(e.target.value))} inputMode="numeric" placeholder="000000" />
          </div>
          <button onClick={disable} disabled={busy || code.length < 6} style={{ justifySelf: "start" }}>
            Tắt 2FA
          </button>
        </div>
      ) : setup ? (
        <div className="card stack">
          <p className="muted">1. Thêm khoá bí mật này vào app xác thực (hoặc quét URI otpauth):</p>
          <div
            className="num"
            style={{
              fontSize: 17,
              letterSpacing: "0.08em",
              wordBreak: "break-all",
              background: "var(--bg)",
              padding: "12px 14px",
              borderRadius: "var(--radius-sm)",
              border: "1px solid var(--border)",
            }}
          >
            {setup.secret}
          </div>
          <p className="faint" style={{ fontSize: 12, wordBreak: "break-all", margin: 0 }}>{setup.otpauthUri}</p>
          <p className="muted">2. Nhập mã 6 số đang hiển thị để xác nhận:</p>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="cc">Mã xác thực</label>
            <input id="cc" className="num" value={code} onChange={(e) => setCode(onlyDigits(e.target.value))} inputMode="numeric" placeholder="000000" autoFocus />
          </div>
          <button className="primary" onClick={confirm} disabled={busy || code.length < 6}>
            Xác nhận &amp; bật
          </button>
        </div>
      ) : (
        <div className="card stack">
          <span className="tag" style={{ justifySelf: "start" }}>Chưa bật</span>
          <button className="primary" onClick={startSetup} disabled={busy} style={{ justifySelf: "start" }}>
            Bật 2FA
          </button>
        </div>
      )}
    </div>
  );
}
