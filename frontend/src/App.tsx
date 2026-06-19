import { useState } from "react";
import { AuthProvider, useAuth } from "./auth";
import { Login } from "./views/Login";
import { Dashboard } from "./views/Dashboard";
import { AccountView } from "./views/AccountView";
import { Transfer } from "./views/Transfer";
import { StandingOrders } from "./views/StandingOrders";
import { Audit } from "./views/Audit";
import type { Notify } from "./ui";

type View =
  | { name: "dashboard" }
  | { name: "account"; id: string }
  | { name: "transfer" }
  | { name: "standing" }
  | { name: "audit" };
type ToastState = { msg: string; kind: "ok" | "err" } | null;

export default function App() {
  return (
    <AuthProvider>
      <Root />
    </AuthProvider>
  );
}

function Root() {
  const { token } = useAuth();
  const [toast, setToast] = useState<ToastState>(null);
  const notify: Notify = (msg, kind = "ok") => {
    setToast({ msg, kind });
    window.setTimeout(() => setToast(null), 3500);
  };

  return (
    <>
      {token ? <AppShell notify={notify} /> : <Login notify={notify} />}
      {toast && (
        <div className={`toast ${toast.kind}`} role="status">
          {toast.msg}
        </div>
      )}
    </>
  );
}

function AppShell({ notify }: { notify: Notify }) {
  const { username, logout } = useAuth();
  const [view, setView] = useState<View>({ name: "dashboard" });

  const section = view.name === "account" ? "dashboard" : view.name;

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <span className="ticks">≣</span> Ledger
        </div>
        <nav className="nav">
          <button className={section === "dashboard" ? "active" : ""} onClick={() => setView({ name: "dashboard" })}>
            Tổng quan
          </button>
          <button className={section === "transfer" ? "active" : ""} onClick={() => setView({ name: "transfer" })}>
            Chuyển tiền
          </button>
          <button className={section === "standing" ? "active" : ""} onClick={() => setView({ name: "standing" })}>
            Định kỳ
          </button>
          <button className={section === "audit" ? "active" : ""} onClick={() => setView({ name: "audit" })}>
            Kiểm toán
          </button>
        </nav>
        <div className="row">
          <span className="muted hide-sm">{username}</span>
          <button className="ghost" onClick={logout}>
            Đăng xuất
          </button>
        </div>
      </header>
      <main className="content">
        {view.name === "dashboard" && (
          <Dashboard notify={notify} onOpenAccount={(id) => setView({ name: "account", id })} />
        )}
        {view.name === "account" && (
          <AccountView accountId={view.id} notify={notify} onBack={() => setView({ name: "dashboard" })} />
        )}
        {view.name === "transfer" && <Transfer notify={notify} onDone={() => setView({ name: "dashboard" })} />}
        {view.name === "standing" && <StandingOrders notify={notify} />}
        {view.name === "audit" && <Audit notify={notify} />}
      </main>
    </div>
  );
}
