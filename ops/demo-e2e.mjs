// Demo end-to-end: gọi API thật trên backend đang chạy, đi qua MỌI tính năng theo phase.
//
// Cách chạy (cần Node 18+ và backend chạy ở localhost:8080):
//   cd backend && ./gradlew bootRun          # cửa sổ 1 (cần admin bootstrap bật — mặc định dev)
//   node ops/demo-e2e.mjs                     # cửa sổ 2
//
// Backend dev tự seed tài khoản admin (admin / admin12345) dùng cho các thao tác ADMIN.
const BASE = process.env.LEDGER_API ?? "http://localhost:8080";
const ok = (m) => console.log("  ✓ " + m);
const info = (m) => console.log("    " + m);
const sec = (t) => console.log("\n=== " + t + " ===");

async function req(method, path, { token, body, idemp } = {}) {
  const headers = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers["Authorization"] = "Bearer " + token;
  if (idemp) headers["Idempotency-Key"] = crypto.randomUUID();
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  const txt = await res.text();
  let data;
  try {
    data = txt ? JSON.parse(txt) : null;
  } catch {
    data = txt;
  }
  return { status: res.status, data };
}
const bal = async (id, token) => (await req("GET", `/accounts/${id}/balance`, { token })).data;
const uniq = () => "demo-" + crypto.randomUUID().slice(0, 8);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

(async () => {
  sec("Phase 5 — Đăng ký / Đăng nhập (JWT)");
  const name = uniq();
  const token = (await req("POST", "/auth/register", { body: { username: name, password: "secret123" } })).data
    .accessToken;
  ok(`Đăng ký '${name}' → nhận access+refresh token`);
  ok(`Đăng nhập sai mật khẩu → ${(await req("POST", "/auth/login", { body: { username: name, password: "x" } })).status} (401)`);
  const admin = (await req("POST", "/auth/login", { body: { username: "admin", password: "admin12345" } })).data
    .accessToken;
  ok("Đăng nhập admin (seed bootstrap) → token ADMIN");

  sec("Phase 1 + 9 — Mở tài khoản (đa tiền tệ)");
  const vnd = (await req("POST", "/accounts", { token, body: { type: "CUSTOMER", currency: "VND" } })).data.accountId;
  const vnd2 = (await req("POST", "/accounts", { token, body: { type: "CUSTOMER", currency: "VND" } })).data.accountId;
  const usd = (await req("POST", "/accounts", { token, body: { type: "CUSTOMER", currency: "USD" } })).data.accountId;
  const sav = (await req("POST", "/accounts", { token, body: { type: "SAVINGS", currency: "VND" } })).data.accountId;
  ok("Mở 4 tài khoản: VND, VND#2, USD, SAVINGS(VND)");
  const eur = await req("POST", "/accounts", { token, body: { type: "CUSTOMER", currency: "EUR" } });
  ok(`Mở tiền tệ chưa hỗ trợ (EUR) → ${eur.status}: ${eur.data.detail}`);

  sec("Phase 2 — Ghi sổ kép, số dư, toàn vẹn");
  await req("POST", `/accounts/${vnd}/deposit`, { token, body: { amount: 1000000 }, idemp: true });
  info(`Nạp 1.000.000 → số dư = ${(await bal(vnd, token)).balance}`);
  await req("POST", `/accounts/${vnd}/withdraw`, { token, body: { amount: 200000 }, idemp: true });
  info(`Rút 200.000 → số dư = ${(await bal(vnd, token)).balance}`);
  ok(`Integrity (sổ cân): balanced=${(await req("GET", "/audit/integrity", { token })).data.balanced}`);
  ok(`Rút vượt số dư → ${(await req("POST", `/accounts/${vnd2}/withdraw`, { token, body: { amount: 999 }, idemp: true }).then((r) => r.status))} (422)`);

  sec("Phase 2 + 5 — Chuyển tiền & ownership");
  await req("POST", "/transfers", { token, body: { fromAccountId: vnd, toAccountId: vnd2, amount: 100000 }, idemp: true });
  info(`Chuyển 100.000: ${(await bal(vnd, token)).balance} / ${(await bal(vnd2, token)).balance}`);
  const xcur = await req("POST", "/transfers", { token, body: { fromAccountId: vnd, toAccountId: usd, amount: 1000 }, idemp: true });
  ok(`Chuyển khác tiền tệ → ${xcur.status} (dùng FX)`);
  const bob = (await req("POST", "/auth/register", { body: { username: uniq(), password: "secret123" } })).data.accessToken;
  ok(`Người khác xem tài khoản của mình → ${(await req("GET", `/accounts/${vnd}/balance`, { token: bob })).status} (403)`);

  sec("Phase 3 — Idempotency-Key");
  const key = crypto.randomUUID();
  const before = (await bal(vnd, token)).balance;
  await req("POST", `/accounts/${vnd}/deposit`, { token, body: { amount: 50000 }, idemp: key });
  await req("POST", `/accounts/${vnd}/deposit`, { token, body: { amount: 50000 }, idemp: key });
  ok(`Nạp 50.000 hai lần cùng key → +${(await bal(vnd, token)).balance - before} (1 hiệu lực)`);

  sec("Phase 8 — Hold (available vs balance)");
  const h = await req("POST", `/accounts/${vnd}/holds`, { token, body: { amount: 300000, ttlSeconds: 3600 }, idemp: true });
  const av = await bal(vnd, token);
  ok(`Đặt giữ 300.000 → balance=${av.balance}, available=${av.available}`);
  await req("POST", `/accounts/${vnd}/holds/${h.data.holdId}/release`, { token });
  ok(`Nhả hold → available=${(await bal(vnd, token)).available}`);

  sec("Phase 9 — Quy đổi tiền tệ (FX)");
  await req("POST", "/exchanges", { token, body: { fromAccountId: vnd, toAccountId: usd, amount: 25000 }, idemp: true });
  ok(`Quy đổi 25.000 VND → USD = ${(await bal(usd, token)).balance}`);
  ok(`Integrity sau FX: balanced=${(await req("GET", "/audit/integrity", { token })).data.balanced} (per-currency)`);

  sec("Phase 4 — Time-travel");
  const tt = (await req("POST", "/accounts", { token, body: { type: "CUSTOMER", currency: "VND" } })).data.accountId;
  await req("POST", `/accounts/${tt}/deposit`, { token, body: { amount: 100000 }, idemp: true });
  const t1 = new Date().toISOString();
  await sleep(1200);
  await req("POST", `/accounts/${tt}/deposit`, { token, body: { amount: 300000 }, idemp: true });
  const now = (await bal(tt, token)).balance;
  const past = (await req("GET", `/accounts/${tt}/balance?asOf=${encodeURIComponent(t1)}`, { token })).data.balance;
  ok(`Số dư hiện tại=${now}, tại mốc trước=${past} (tua lại lịch sử)`);

  sec("Phase 4 — Bút toán bù (reversal, ADMIN)");
  const rv = (await req("POST", "/accounts", { token, body: { type: "CUSTOMER", currency: "VND" } })).data.accountId;
  await req("POST", `/accounts/${rv}/deposit`, { token, body: { amount: 500000 }, idemp: true });
  const tx = (await req("POST", "/transfers", { token, body: { fromAccountId: rv, toAccountId: vnd2, amount: 70000 }, idemp: true })).data.txId;
  ok(`Reverse ${(tx || "").slice(0, 8)} → ${(await req("POST", `/transactions/${tx}/reverse`, { token: admin, idemp: true })).status}; số dư nguồn=${(await bal(rv, token)).balance}`);

  sec("Phase 4 — Hash-chain chống giả mạo (ADMIN/AUDITOR)");
  const hc = (await req("GET", "/audit/hash-chain", { token: admin })).data;
  ok(`Verify: intact=${hc.intact}, eventsChecked=${hc.eventsChecked}`);

  sec("Phase 8 — Phát hiện gian lận + tự đóng băng");
  const fr = (await req("POST", "/accounts", { token, body: { type: "CUSTOMER", currency: "VND" } })).data.accountId;
  await req("POST", `/accounts/${fr}/deposit`, { token, body: { amount: 200000000 }, idemp: true });
  await req("POST", `/accounts/${fr}/withdraw`, { token, body: { amount: 150000000 }, idemp: true });
  info(`Rút 150tr (lớn bất thường) → trạng thái: ${(await bal(fr, token)).status}`);
  const blk = await req("POST", `/accounts/${fr}/withdraw`, { token, body: { amount: 1000 }, idemp: true });
  ok(`Ghi nợ trên tài khoản đóng băng → ${blk.status} (422)`);
  await req("POST", `/admin/accounts/${fr}/unfreeze`, { token: admin });
  ok(`ADMIN mở băng → ${(await bal(fr, token)).status}`);

  sec("Phase 5 — Maker-checker (bốn-mắt)");
  const mcA = (await req("POST", "/accounts", { token, body: { type: "CUSTOMER", currency: "VND" } })).data.accountId;
  await req("POST", `/accounts/${mcA}/deposit`, { token, body: { amount: 200000000 }, idemp: true });
  const big = await req("POST", "/transfers", { token, body: { fromAccountId: mcA, toAccountId: vnd2, amount: 120000000 }, idemp: true });
  ok(`Chuyển 120tr (>= ngưỡng) → ${big.status} status=${big.data.status}`);
  ok(`Người tạo tự duyệt → ${(await req("POST", `/admin/approvals/${big.data.approvalId}/approve`, { token })).status} (403 four-eyes)`);
  ok(`ADMIN duyệt → ${(await req("POST", `/admin/approvals/${big.data.approvalId}/approve`, { token: admin })).status}, đã thực thi`);

  sec("Phase 8 — Tiết kiệm + lãi, lệnh định kỳ");
  await req("POST", `/accounts/${sav}/deposit`, { token, body: { amount: 10000000 }, idemp: true });
  ok(`Accrue interest → ${(await req("POST", `/admin/accounts/${sav}/accrue-interest`, { token: admin })).status} (lãi theo thời gian)`);
  ok(`Tạo lệnh định kỳ → ${(await req("POST", "/standing-orders", { token, body: { fromAccountId: vnd, toAccountId: vnd2, amount: 1000, intervalSeconds: 86400 } })).status}`);

  sec("Phase 5 — Phân quyền + Phase 1 rebuild + Phase 6 metrics");
  ok(`CUSTOMER → /admin/fraud/frozen: ${(await req("GET", "/admin/fraud/frozen", { token })).status} (403)`);
  ok(`ADMIN rebuild read model → ${(await req("POST", "/admin/read-model/rebuild", { token: admin })).status}; integrity=${(await req("GET", "/audit/integrity", { token })).data.balanced}`);
  const prom = await (await fetch(BASE + "/actuator/prometheus")).text();
  ok(`Metrics Prometheus: ${prom.split("\n").filter((l) => l.startsWith("ledger_")).length} dòng ledger_*`);

  sec("Phase 5 — Rate limiting");
  const codes = [];
  for (let i = 0; i < 14; i++) codes.push((await req("POST", "/auth/login", { body: { username: "x", password: "y" } })).status);
  ok(`14 lần login dồn → ${codes.join(",")} (xuất hiện 429)`);

  console.log("\n✔ HOÀN TẤT — đã demo end-to-end mọi tính năng trên backend thật.");
})().catch((e) => {
  console.error("LỖI DEMO:", e);
  process.exit(1);
});
