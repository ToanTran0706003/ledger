// Client gọi API Ledger. Gắn Bearer token + sinh Idempotency-Key cho thao tác ghi tiền.
const BASE = (import.meta.env.VITE_API_URL as string | undefined) ?? "http://localhost:8080";

/** Tiền tệ hỗ trợ (khớp ledger.vault.currencies ở backend). */
export const CURRENCIES = ["VND", "USD"];

export type Tokens = { accessToken: string; refreshToken: string; tokenType: string };
export type Account = {
  accountId: string;
  owner: string;
  type: string;
  currency: string;
  balance: number;
  available: number;
  status: string;
  freezeReason: string | null;
};
export type StandingOrderView = {
  id: string;
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  intervalSeconds: number;
  nextRunAt: string;
  active: boolean;
};
export type HoldView = {
  holdId: string;
  accountId: string;
  amount: number;
  status: string; // ACTIVE | RELEASED | CAPTURED
  reason: string | null;
  placedAt: string;
  expiresAt: string;
  releasedAt: string | null;
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
export type IntegrityReport = { totalBalance: number; expectedTotal: number; balanced: boolean };
export type HashChainReport = { intact: boolean; eventsChecked: number; firstBrokenSeq: number | null };
export type FrozenAccount = { accountId: string; owner: string; freezeReason: string | null };
export type TransferResult = { status: "EXECUTED" | "PENDING_APPROVAL"; txId: string | null; approvalId: string | null };
export type PendingApproval = {
  id: string;
  makerUserId: string;
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  createdAt: string;
};

export class ApiError extends Error {
  status: number;
  twoFactorRequired: boolean;
  constructor(status: number, message: string, twoFactorRequired = false) {
    super(message);
    this.status = status;
    this.twoFactorRequired = twoFactorRequired;
  }
}

export type TwoFactorSetup = { secret: string; otpauthUri: string };

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
    let detail: string | null = null;
    let twoFactorRequired = false;
    try {
      const j = await res.json();
      detail = j.detail ?? j.message ?? null;
      twoFactorRequired = j.twoFactorRequired === true;
    } catch {
      /* body không phải JSON */
    }
    // Giữ thông điệp domain của backend (đăng nhập sai, mã 2FA…); chỉ fallback khi không có body
    // (vd token hết hạn do Spring Security trả 401 rỗng).
    if (detail == null) {
      detail = res.status === 401 ? "Phiên đăng nhập không hợp lệ hoặc đã hết hạn." : `Có lỗi xảy ra (mã ${res.status}).`;
    }
    throw new ApiError(res.status, detail, twoFactorRequired);
  }

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

export const api = {
  register: (username: string, password: string) =>
    request<Tokens>("/auth/register", { method: "POST", body: { username, password }, auth: false }),
  login: (username: string, password: string, totpCode?: string) =>
    request<Tokens>("/auth/login", { method: "POST", body: { username, password, totpCode }, auth: false }),
  logout: () => request<void>("/auth/logout", { method: "POST" }),
  twoFactorStatus: () => request<{ enabled: boolean }>("/auth/2fa/status"),
  setup2fa: () => request<TwoFactorSetup>("/auth/2fa/setup", { method: "POST" }),
  enable2fa: (code: string) => request<void>("/auth/2fa/enable", { method: "POST", body: { code } }),
  disable2fa: (code: string) => request<void>("/auth/2fa/disable", { method: "POST", body: { code } }),
  myAccounts: () => request<Account[]>("/accounts"),
  openAccount: (type: string, currency: string) =>
    request<{ accountId: string }>("/accounts", { method: "POST", body: { type, currency } }),
  balance: (id: string) => request<Account>(`/accounts/${id}/balance`),
  balanceAsOf: (id: string, asOf: string) =>
    request<BalanceAt>(`/accounts/${id}/balance?asOf=${encodeURIComponent(asOf)}`),
  history: (id: string) => request<HistoryRow[]>(`/accounts/${id}/history`),
  integrity: () => request<IntegrityReport>("/audit/integrity"),
  hashChain: () => request<HashChainReport>("/audit/hash-chain"),
  frozenAccounts: () => request<FrozenAccount[]>("/admin/fraud/frozen"),
  unfreezeAccount: (id: string) => request<void>(`/admin/accounts/${id}/unfreeze`, { method: "POST" }),
  standingOrders: () => request<StandingOrderView[]>("/standing-orders"),
  createStandingOrder: (fromAccountId: string, toAccountId: string, amount: number, intervalSeconds: number) =>
    request<{ id: string }>("/standing-orders", {
      method: "POST",
      body: { fromAccountId, toAccountId, amount, intervalSeconds },
    }),
  deposit: (id: string, amount: number) =>
    request<{ txId: string }>(`/accounts/${id}/deposit`, { method: "POST", body: { amount }, idempotent: true }),
  withdraw: (id: string, amount: number) =>
    request<{ txId: string }>(`/accounts/${id}/withdraw`, { method: "POST", body: { amount }, idempotent: true }),
  transfer: (fromAccountId: string, toAccountId: string, amount: number) =>
    request<TransferResult>("/transfers", {
      method: "POST",
      body: { fromAccountId, toAccountId, amount },
      idempotent: true,
    }),
  pendingApprovals: () => request<PendingApproval[]>("/admin/approvals"),
  approveTransfer: (id: string) => request<{ txId: string }>(`/admin/approvals/${id}/approve`, { method: "POST" }),
  rejectTransfer: (id: string) => request<void>(`/admin/approvals/${id}/reject`, { method: "POST" }),
  exchange: (fromAccountId: string, toAccountId: string, amount: number) =>
    request<{ txId: string }>("/exchanges", {
      method: "POST",
      body: { fromAccountId, toAccountId, amount },
      idempotent: true,
    }),
  holds: (id: string) => request<HoldView[]>(`/accounts/${id}/holds`),
  placeHold: (id: string, amount: number, ttlSeconds: number) =>
    request<{ holdId: string }>(`/accounts/${id}/holds`, {
      method: "POST",
      body: { amount, ttlSeconds },
      idempotent: true,
    }),
  releaseHold: (id: string, holdId: string) =>
    request<void>(`/accounts/${id}/holds/${holdId}/release`, { method: "POST" }),
  captureHold: (id: string, holdId: string) =>
    request<{ txId: string }>(`/accounts/${id}/holds/${holdId}/capture`, { method: "POST", idempotent: true }),
};
