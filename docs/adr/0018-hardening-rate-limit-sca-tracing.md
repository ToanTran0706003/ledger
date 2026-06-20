# ADR-0018: Hardening — rate limiting, quét phụ thuộc (OWASP), tracing

## Trạng thái
Accepted

## Bối cảnh
Dọn nợ "chuẩn doanh nghiệp" còn lại từ Phase 5/6: giới hạn tần suất (chống dò mật khẩu / lạm
dụng), quét lỗ hổng phụ thuộc (SCA) trong CI, và tracing phân tán (OpenTelemetry).

## Quyết định

### 1. Rate limiting (token-bucket trong bộ nhớ, theo IP)
- Hai xô token-bucket: **auth** (chặt — `/auth/**` permitAll, chống dò mật khẩu) và **write**
  (rộng hơn — lệnh ghi đã xác thực, chặn lạm dụng theo phiên). Đọc (GET) không giới hạn.
- `HandlerInterceptor` ném `RateLimitExceededException` → **HTTP 429** (ProblemDetail). Key là
  **`request.getRemoteAddr()`** (IP peer thật) — **KHÔNG tin `X-Forwarded-For`** do client tự đặt
  (giả mạo được để vượt mặt limiter); sau proxy tin cậy thì bật `server.forward-headers-strategy`.
- Backstop chống map phình (cardinality cao bất thường → xóa nhẹ). Cấu hình `ledger.rate-limit.*`,
  tắt trong test. **Một node**; đa node / quy mô lớn cần backend chia sẻ (Redis) — chưa làm.

### 2. OWASP Dependency-Check (SCA) trong CI
- Workflow **lên lịch riêng** (`dependency-check.yml`, hằng tuần + chạy tay), **tách khỏi job
  build-test bắt buộc**: SCA tốn thời gian (tải NVD) nên không chặn mỗi push. Cần secret
  `NVD_API_KEY` (free) để nhanh & tránh rate-limit; kết quả SARIF đẩy lên Code scanning;
  `--failOnCVSS 7`.

### 3. OpenTelemetry tracing — **hoãn sang Phase 9 (distributed)**
- Lý do: giá trị của tracing là **nối span xuyên nhiều service**. Hiện hệ là **modular monolith**
  và **đã có** correlationId/userId (qua MDC) + metrics Prometheus (độ trễ/throughput/lag) để tương
  quan request và đo hiệu năng. Trong một tiến trình, OTel có giá trị biên thấp và khó verify ý
  nghĩa (cần collector). Thêm OTel khi **tách service** (Phase 9) — đúng lúc span thực sự vượt ranh
  giới và trace phân tán trả công.

## Hệ quả
**Được:** chống dò mật khẩu + lạm dụng; lộ CVE phụ thuộc theo lịch mà không làm chậm CI; tracing
để đúng nơi nó hữu ích. **Mất / đánh đổi:** limiter một-node trong bộ nhớ (Redis cho đa node);
SCA chỉ chạy theo lịch (không chặn merge); OTel chưa có (chấp nhận tới Phase 9).

## Phương án đã cân nhắc
- **Bucket4j / Redis cho rate limit** — loại lúc này: thêm dependency/hạ tầng cho một node; tự
  cài token-bucket gọn, đúng, dễ giải thích.
- **OWASP gradle plugin trong build bắt buộc** — loại: làm chậm MỌI lần CI + ràng buộc NVD; chạy
  theo lịch tách biệt an toàn hơn cho vòng lặp dev.
- **Thêm OpenTelemetry ngay** — hoãn: giá trị biên thấp ở monolith; để Phase 9.
