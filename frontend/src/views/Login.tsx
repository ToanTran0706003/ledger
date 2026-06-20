import { useState } from "react";
import type { FormEvent } from "react";
import { useAuth } from "../auth";
import { ApiError } from "../api";
import type { Notify } from "../ui";

export function Login({ notify }: { notify: Notify }) {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [totpCode, setTotpCode] = useState("");
  const [needsTotp, setNeedsTotp] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErr(null);
    setBusy(true);
    try {
      if (mode === "login") await login(username, password, needsTotp ? totpCode : undefined);
      else await register(username, password);
      notify(mode === "login" ? "Đã đăng nhập." : "Đã tạo tài khoản.");
    } catch (ex) {
      if (ex instanceof ApiError && ex.twoFactorRequired) {
        setNeedsTotp(true);
        setErr("Nhập mã 6 số từ ứng dụng xác thực.");
      } else {
        setErr(ex instanceof ApiError ? ex.message : "Không kết nối được máy chủ.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth">
      <div className="auth-grid">
        <div className="auth-form">
          <div className="brand" style={{ marginBottom: 20, fontSize: 20 }}>
            <span className="ticks">≣</span> Ledger
          </div>
          <h1>{mode === "login" ? "Đăng nhập" : "Tạo tài khoản"}</h1>
          <p className="muted" style={{ marginTop: 8 }}>
            Lõi sổ cái tài chính. Sự kiện bất biến, sổ luôn cân.
          </p>
          <form className="card stack" style={{ marginTop: 20 }} onSubmit={submit}>
            <div className="field">
              <label htmlFor="u">Tên đăng nhập</label>
              <input id="u" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required minLength={3} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="p">Mật khẩu</label>
              <input
                id="p"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete={mode === "login" ? "current-password" : "new-password"}
                required
                minLength={8}
              />
              {err && (
                <div className="error" role="alert">
                  {err}
                </div>
              )}
            </div>
            {needsTotp && mode === "login" && (
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="totp">Mã xác thực 2 lớp</label>
                <input
                  id="totp"
                  className="num"
                  value={totpCode}
                  onChange={(e) => setTotpCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  placeholder="000000"
                  autoFocus
                />
              </div>
            )}
            <button className="primary" type="submit" disabled={busy}>
              {busy ? "Đang xử lý" : mode === "login" ? "Đăng nhập" : "Tạo tài khoản"}
            </button>
          </form>
          <p className="muted" style={{ marginTop: 16 }}>
            {mode === "login" ? "Chưa có tài khoản? " : "Đã có tài khoản? "}
            <button
              className="ghost"
              type="button"
              onClick={() => {
                setMode(mode === "login" ? "register" : "login");
                setErr(null);
                setNeedsTotp(false);
                setTotpCode("");
              }}
            >
              {mode === "login" ? "Tạo mới" : "Đăng nhập"}
            </button>
          </p>
        </div>

        {/* Điểm nhấn: số dư KHÔNG được lưu — nó dựng lại từ chuỗi sự kiện bất biến. */}
        <aside className="auth-aside" aria-hidden="true">
          <div className="eyebrow">Số dư dựng từ chuỗi sự kiện</div>
          <div className="auth-events">
            <div className="auth-ev">
              <span>Mở tài khoản</span>
              <span className="num faint">0 ₫</span>
            </div>
            <div className="auth-ev">
              <span>
                Nạp tiền <em className="credit">+1.000.000</em>
              </span>
              <span className="num">1.000.000 ₫</span>
            </div>
            <div className="auth-ev">
              <span>
                Rút tiền <em className="debit">−250.000</em>
              </span>
              <span className="num">750.000 ₫</span>
            </div>
            <div className="auth-ev">
              <span>
                Chuyển tiền <em className="debit">−300.000</em>
              </span>
              <span className="num">450.000 ₫</span>
            </div>
          </div>
          <div className="auth-eq">
            số dư <span className="faint">=</span> <span className="num accent-text">Σ</span> sự kiện
          </div>
        </aside>
      </div>
    </div>
  );
}
