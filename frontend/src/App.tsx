import { useState } from "react";
import { AuthProvider, useAuth } from "./auth";
import { Login } from "./views/Login";
import { Dashboard } from "./views/Dashboard";
import { AccountView } from "./views/AccountView";
import type { Notify } from "./ui";

type View = { name: "dashboard" } | { name: "account"; id: string };
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

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <span className="ticks">≣</span> Ledger
        </div>
        <div className="row">
          <span className="muted hide-sm">{username}</span>
          <button className="ghost" onClick={logout}>
            Đăng xuất
          </button>
        </div>
      </header>
      <main className="content">
        {view.name === "dashboard" ? (
          <Dashboard notify={notify} onOpenAccount={(id) => setView({ name: "account", id })} />
        ) : (
          <AccountView accountId={view.id} notify={notify} onBack={() => setView({ name: "dashboard" })} />
        )}
      </main>
    </div>
  );
}
