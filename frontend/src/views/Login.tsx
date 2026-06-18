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
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErr(null);
    setBusy(true);
    try {
      if (mode === "login") await login(username, password);
      else await register(username, password);
      notify(mode === "login" ? "Đã đăng nhập." : "Đã tạo tài khoản.");
    } catch (ex) {
      setErr(ex instanceof ApiError ? ex.message : "Không kết nối được máy chủ.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="content" style={{ maxWidth: 420, marginTop: "8vh" }}>
      <div className="brand" style={{ marginBottom: 24, fontSize: 20 }}>
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
        <div className="field">
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
          }}
        >
          {mode === "login" ? "Tạo mới" : "Đăng nhập"}
        </button>
      </p>
    </div>
  );
}
