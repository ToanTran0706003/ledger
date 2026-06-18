// Client gọi API Ledger. Gắn Bearer token + sinh Idempotency-Key cho thao tác ghi tiền.
const BASE = (import.meta.env.VITE_API_URL as string | undefined) ?? "http://localhost:8080";

export type Tokens = { accessToken: string; refreshToken: string; tokenType: string };
export type Account = {
  accountId: string;
  owner: string;
  currency: string;
  balance: number;
  available: number;
  status: string;
};
export type HistoryRow = {
  txId: string;
  direction: "C" | "D";
  amount: number;
  counterparty: string | null;
  balanceAfter: number;
  movementType: string;
  occurredAt: string;
};
export type BalanceAt = { accountId: string; asOf: string; balance: number };

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

let tokenGetter: () => string | null = () => null;
export function setTokenGetter(fn: () => string | null) {
  tokenGetter = fn;
}

type Opts = { method?: string; body?: unknown; idempotent?: boolean; auth?: boolean };

async function request<T>(path: string, opts: Opts = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (opts.body !== undefined) headers["Content-Type"] = "application/json";
  if (opts.auth !== false) {
    const t = tokenGetter();
    if (t) headers["Authorization"] = `Bearer ${t}`;
  }
  if (opts.idempotent) headers["Idempotency-Key"] = crypto.randomUUID();

  const res = await fetch(`${BASE}${path}`, {
    method: opts.method ?? "GET",
    headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  });

  if (!res.ok) {
    let detail = `Có lỗi xảy ra (mã ${res.status}).`;
    try {
      const j = await res.json();
      detail = j.detail ?? j.message ?? detail;
    } catch {
      /* body không phải JSON */
    }
    if (res.status === 401) detail = "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.";
    throw new ApiError(res.status, detail);
  }

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

export const api = {
  register: (username: string, password: string) =>
    request<Tokens>("/auth/register", { method: "POST", body: { username, password }, auth: false }),
  login: (username: string, password: string) =>
    request<Tokens>("/auth/login", { method: "POST", body: { username, password }, auth: false }),
  myAccounts: () => request<Account[]>("/accounts"),
  openAccount: () => request<{ accountId: string }>("/accounts", { method: "POST", body: { type: "CUSTOMER" } }),
  balance: (id: string) => request<Account>(`/accounts/${id}/balance`),
  balanceAsOf: (id: string, asOf: string) =>
    request<BalanceAt>(`/accounts/${id}/balance?asOf=${encodeURIComponent(asOf)}`),
  history: (id: string) => request<HistoryRow[]>(`/accounts/${id}/history`),
  deposit: (id: string, amount: number) =>
    request<{ txId: string }>(`/accounts/${id}/deposit`, { method: "POST", body: { amount }, idempotent: true }),
  withdraw: (id: string, amount: number) =>
    request<{ txId: string }>(`/accounts/${id}/withdraw`, { method: "POST", body: { amount }, idempotent: true }),
  transfer: (fromAccountId: string, toAccountId: string, amount: number) =>
    request<{ txId: string }>("/transfers", {
      method: "POST",
      body: { fromAccountId, toAccountId, amount },
      idempotent: true,
    }),
};
