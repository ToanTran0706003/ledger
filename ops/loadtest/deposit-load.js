// Load test đồng thời cho luồng nạp tiền. Chạy: k6 run ops/loadtest/deposit-load.js
// Backend mặc định http://localhost:8080 (đổi qua biến môi trường BASE_URL).
import http from "k6/http";
import { check, sleep } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";

const BASE = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  scenarios: {
    deposits: {
      executor: "ramping-vus",
      startVUs: 1,
      stages: [
        { duration: "15s", target: 20 },
        { duration: "30s", target: 20 },
        { duration: "15s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<200", "p(99)<400"],
  },
};

// Mỗi VU: đăng ký user riêng + mở tài khoản một lần (setup theo VU).
export function setup() {
  return {};
}

export default function () {
  const username = `k6_${uuidv4()}`;
  const reg = http.post(`${BASE}/auth/register`, JSON.stringify({ username, password: "password123" }), {
    headers: { "Content-Type": "application/json" },
  });
  check(reg, { "register 201": (r) => r.status === 201 });
  const token = reg.json("accessToken");
  const authJson = { "Content-Type": "application/json", Authorization: `Bearer ${token}` };

  const open = http.post(`${BASE}/accounts`, JSON.stringify({ type: "CUSTOMER" }), { headers: authJson });
  const accountId = open.json("accountId");

  for (let i = 0; i < 5; i++) {
    const res = http.post(
      `${BASE}/accounts/${accountId}/deposit`,
      JSON.stringify({ amount: 10 }),
      { headers: { ...authJson, "Idempotency-Key": uuidv4() } },
    );
    check(res, { "deposit 200": (r) => r.status === 200 });
  }
  sleep(0.5);
}
