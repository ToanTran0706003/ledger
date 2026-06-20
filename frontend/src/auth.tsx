import { createContext, useContext, useState } from "react";
import type { ReactNode } from "react";
import { api, setTokenGetter } from "./api";
import type { Tokens } from "./api";

type AuthState = {
  username: string | null;
  token: string | null;
  roles: string[];
  login: (u: string, p: string, totpCode?: string) => Promise<void>;
  register: (u: string, p: string) => Promise<void>;
  logout: () => void;
};

/** Đọc các vai trò từ claim "roles" của JWT (base64url). Lỗi giải mã -> không vai trò. */
function rolesFromToken(token: string | null): string[] {
  if (!token) return [];
  try {
    const part = token.split(".")[1];
    if (!part) return [];
    // base64url -> base64 + bù padding; giải mã UTF-8 an toàn (username có thể có dấu).
    const b64 = part.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(part.length / 4) * 4, "=");
    const json = new TextDecoder().decode(Uint8Array.from(atob(b64), (c) => c.charCodeAt(0)));
    const claims = JSON.parse(json);
    return Array.isArray(claims.roles) ? claims.roles : [];
  } catch {
    return [];
  }
}

const AuthContext = createContext<AuthState | null>(null);

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth phải dùng trong AuthProvider");
  return ctx;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem("ledger.token"));
  const [username, setUsername] = useState<string | null>(() => localStorage.getItem("ledger.user"));

  // Đặt token getter đồng bộ mỗi lần render, trước khi view con gọi API.
  setTokenGetter(() => token);

  function apply(t: Tokens, u: string) {
    localStorage.setItem("ledger.token", t.accessToken);
    localStorage.setItem("ledger.user", u);
    setToken(t.accessToken);
    setUsername(u);
  }

  const value: AuthState = {
    username,
    token,
    roles: rolesFromToken(token),
    login: async (u, p, totpCode) => apply(await api.login(u, p, totpCode), u),
    register: async (u, p) => apply(await api.register(u, p), u),
    logout: () => {
      // Thu hồi refresh token phía máy chủ (best-effort) rồi xoá phiên cục bộ.
      api.logout().catch(() => {});
      localStorage.removeItem("ledger.token");
      localStorage.removeItem("ledger.user");
      setToken(null);
      setUsername(null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
