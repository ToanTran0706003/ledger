# Ledger — Bản đồ tài liệu

> Bộ tài liệu nền tảng của dự án. Tổng quan dự án và cách chạy nằm ở [README gốc](../README.md).

## Tài liệu này dành cho ai đọc?

- **Nhà tuyển dụng / reviewer kỹ thuật**: đọc theo thứ tự 00 → 02 → 04 để nắm định vị, kiến trúc và phần bảo mật/chống gian lận (nơi giá trị kỹ thuật tập trung).
- **Người phát triển (chính bạn)**: dùng `07-roadmap` và `08-todo-backlog` làm kim chỉ nam thi công; tra cứu `03`, `05`, `09` khi cần chi tiết.

## Bản đồ tài liệu

| File | Nội dung | Khi nào đọc |
|------|----------|-------------|
| [00-vision-and-analysis.md](./00-vision-and-analysis.md) | Định vị dự án, phân tích hiện tại & tương lai, đối tượng, lý do tồn tại, SWOT | Đọc đầu tiên |
| [01-architecture.md](./01-architecture.md) | Kiến trúc tổng thể, modular monolith, luồng CQRS/ES, lộ trình tiến hóa lên microservices | Sau vision |
| [02-domain-and-business.md](./02-domain-and-business.md) | Mô hình nghiệp vụ, double-entry, aggregate/event/command, business rules, vòng đời | Hiểu "dự án làm gì" |
| [03-data-and-eventstore.md](./03-data-and-eventstore.md) | Thiết kế event store, schema, projection, snapshot, event versioning | Khi thiết kế DB |
| [04-security-and-anti-cheat.md](./04-security-and-anti-cheat.md) | Bảo mật, authn/authz, chống gian lận, concurrency, idempotency, toàn vẹn sổ cái | Phần "wao" cốt lõi |
| [05-performance-and-scaling.md](./05-performance-and-scaling.md) | Snapshot, read model, archiving, caching, scaling, benchmark mục tiêu | Khi tối ưu |
| [06-uxui-and-anti-slop.md](./06-uxui-and-anti-slop.md) | Nguyên tắc thiết kế chống "AI slop", design system, accessibility | Khi làm frontend |
| [07-roadmap-and-phases.md](./07-roadmap-and-phases.md) | Lộ trình theo phase, milestone, định nghĩa "done" | Lập kế hoạch thi công |
| [08-todo-backlog.md](./08-todo-backlog.md) | Backlog chi tiết theo phase, checklist từng task | Hàng ngày khi code |
| [09-tech-stack-and-setup.md](./09-tech-stack-and-setup.md) | Stack, lý do chọn, khởi tạo project, CI/CD, deploy $0 | Khi dựng môi trường |
| [10-testing-and-quality.md](./10-testing-and-quality.md) | Chiến lược test, coverage, property-based testing, chất lượng code | Khi viết test |
| [huong-dan-su-dung.md](./huong-dan-su-dung.md) | Hướng dẫn sử dụng Web UI cho khách hàng, quản trị viên và kiểm toán viên | Khi demo hoặc thao tác trên app |
| [adr/](./adr/) | Architecture Decision Records — các quyết định kiến trúc đã chốt | Khi cần "vì sao" |

## Triết lý dự án (TL;DR)

1. **Business cố tình đơn giản, kỹ thuật cố tình sâu.** Nghiệp vụ ai cũng hiểu (nạp/rút/chuyển) để người xem dồn chú ý vào *cách* xử lý.
2. **Không bao giờ UPDATE/DELETE dữ liệu tiền.** Mọi thay đổi là một event bất biến. Sửa sai bằng event bù trừ.
3. **Ghi sổ kép (double-entry).** Tiền không tự sinh/biến mất; tổng hệ thống luôn cân. Có hàm kiểm tra toàn vẹn.
4. **Chất lượng doanh nghiệp.** Test đầy đủ, CI/CD, observability, tài liệu chỉn chu — không phải prototype vứt đi.
5. **Anti-slop.** Mọi quyết định thiết kế (code & UI) đều có lý do, không phải mặc định do AI sinh ra.

## Trạng thái hiện tại

> Cập nhật mục này khi tiến độ thay đổi.

- **Phase hiện tại:** Phase 0–8 hoàn chỉnh ✅ + Phase 9 (đa tiền tệ + FX) ✅ → còn lại: Phase 9 hạ
  tầng (Kafka/microservice/saga — cần Docker) và Phase 10 (GIF/video demo).
- **Tài liệu:** Bộ docs nền tảng + **ADR-0001..0020** + `benchmarks/` đã có.
- **Code:** backend/ (ES/CQRS + double-entry + outbox/retry/idempotency + snapshot/time-travel/reversal
  + JWT/ownership/vai trò + observability + tiết kiệm/lãi & lệnh định kỳ + **hold/reservation +
  hash-chain chống giả mạo + fraud detection/đóng băng + hạn mức ngày + đa tiền tệ/FX + maker-checker**
  + rate-limit + OWASP SCA + console admin/audit) **và** frontend/ (React+TS, 7 màn, signature replay,
  time-travel, FX, trạng thái đóng băng, màn Quản trị). **92 backend test**, CI build cả hai. Chạy: [README gốc](../README.md).
- **Demo:** chạy `node ops/demo-e2e.mjs` (đi qua mọi tính năng trên backend thật); hoặc dùng UI trực tiếp.

## Quy ước tài liệu

- Mọi quyết định kiến trúc quan trọng nên được ghi lại dưới dạng **ADR** (Architecture Decision Record) trong `docs/adr/` — template ở cuối file `01-architecture.md`.
- Sơ đồ dùng cú pháp **Mermaid** để render trực tiếp trên GitHub.
- Thuật ngữ tiếng Anh giữ nguyên (event, aggregate, projection...) vì là chuẩn ngành; giải thích tiếng Việt kèm theo.
