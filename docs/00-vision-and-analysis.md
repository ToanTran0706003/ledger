# 00 — Vision & Phân tích dự án

## 1. Một câu định vị

> **Ledger là lõi sổ cái tài chính (financial ledger core) xây theo Event Sourcing + CQRS, mô phỏng cách một ngân hàng/fintech thật quản lý tiền của khách hàng — với trọng tâm là tính đúng đắn, bất biến, audit và khả năng mở rộng, không phải độ phong phú tính năng.**

## 2. Vấn đề định vị: tại sao chủ đề này?

### Bối cảnh
Thị trường IT cạnh tranh gay gắt. Một lập trình viên chuyển ngạch (C# → Java) cần một dự án portfolio *khác biệt*. Phần lớn ứng viên junior nộp các dự án CRUD: blog, e-commerce cơ bản, todo app. Chúng giống nhau và không thể hiện chiều sâu.

### Lựa chọn chiến lược
Ledger cố tình chọn một **business đơn giản** (nạp/rút/chuyển tiền) nhưng đặt **toàn bộ độ khó vào tầng kỹ thuật**. Đây là quyết định có chủ đích:

- Business đơn giản → người review không bị phân tâm bởi logic nghiệp vụ rối rắm, dồn sự chú ý vào *cách* hệ thống được xây.
- Domain tài chính → mọi sai sót đều "đắt" (mất tiền, sai số dư), buộc phải xử lý đúng những vấn đề khó: concurrency, idempotency, consistency.
- Event Sourcing + CQRS → hai pattern mà junior hiếm khi chạm tới, lại cực kỳ được trọng dụng ở fintech.

### Thông điệp gửi tới nhà tuyển dụng
Không phải *"tôi biết làm app ngân hàng"*, mà là *"tôi hiểu cách tiền thật được quản lý an toàn ở quy mô doanh nghiệp"*.

## 3. Dự án này KHÔNG phải gì (chống hiểu lầm)

| Không phải | Mà là |
|------------|-------|
| Một ngân hàng thật kết nối tiền thật | Một lõi sổ cái mô phỏng, tiền là con số trong DB |
| Cạnh tranh tính năng với Vietcombank/Momo | Bài trình diễn kiến trúc & tư duy hệ thống |
| App CRUD có giao diện đẹp | Hệ thống event-sourced có chiều sâu kỹ thuật |
| Prototype dùng một lần | Sản phẩm chuẩn doanh nghiệp: test, CI/CD, observability |

### "Tiền từ đâu ra?" — câu trả lời kiến trúc
Ledger là **lõi sổ cái**, không phải **cổng kết nối tiền thật** (NAPAS/SWIFT). Tiền được mô hình bằng nguyên tắc **double-entry**: tồn tại một tài khoản hệ thống (`SYSTEM_VAULT`) với số dư khởi tạo lớn. "Nạp tiền" thực chất là chuyển từ vault sang tài khoản khách; "rút" là chiều ngược lại. Nhờ vậy **tổng tiền toàn hệ thống luôn không đổi** — không có tiền tự sinh ra. Chi tiết ở `02-domain-and-business.md`.

## 4. Đối tượng & người dùng

| Nhóm | Họ quan tâm gì | Ta phục vụ thế nào |
|------|----------------|---------------------|
| Nhà tuyển dụng fintech/backend | Chiều sâu kỹ thuật, tư duy hệ thống, chất lượng code | Docs rõ ràng + code sạch + test + ADR |
| Reviewer kỹ thuật (CTO/lead) | Quyết định kiến trúc có lý do, trade-off | ADR, file architecture, security |
| Chính bạn (developer) | Học Java/Spring sâu, có sản phẩm để nói trong phỏng vấn | Roadmap, backlog, setup guide |
| Người dùng demo | Trải nghiệm mượt để cảm nhận sản phẩm | UI anti-slop, API rõ ràng |

## 5. Phân tích hiện trạng (As-Is)

- **Giai đoạn:** Khởi tạo. Đã có ý tưởng rõ, demo concept HTML/JS, và bộ tài liệu nền tảng (chính tài liệu này).
- **Tài sản sẵn có:** Kinh nghiệm 1+ năm C# (ASP.NET Core rất gần Spring), tư duy backend đã có.
- **Khoảng trống:** Chưa quen hệ sinh thái Java/Spring, chưa có code thật, chưa từng làm Event Sourcing production.
- **Ràng buộc:** Làm một mình, budget $0, dùng AI làm công cụ chính → phải bù lại bằng tính chỉn chu và khả năng *giải thích* mọi quyết định.

## 6. Phán đoán tương lai (To-Be) — 3 chân trời

### Chân trời 1 — MVP vững (mục tiêu gần)
Một modular monolith Spring Boot: mở tài khoản, nạp/rút/chuyển, double-entry, event store + read model, snapshot, idempotency, optimistic concurrency, test đầy đủ, deploy được trên free tier. **Đây đã đủ để gây ấn tượng mạnh trong phỏng vấn.**

### Chân trời 2 — Flagship (mục tiêu dự án này)
Thêm: time-travel query, audit dashboard, hàm kiểm tra toàn vẹn sổ cái, sản phẩm tài chính nâng cao (tiết kiệm + tính lãi qua replay, chuyển tiền định kỳ), phát hiện gian lận cơ bản, observability (metrics/tracing/logs), CI/CD hoàn chỉnh, tài liệu chuẩn doanh nghiệp.

### Chân trời 3 — Tiến hóa lên Distributed (mở rộng)
Tách modular monolith thành microservices, đưa Kafka làm event backbone thật, áp dụng Saga cho giao dịch liên service, đa tiền tệ, quy mô hóa read model. Đây là phase tùy chọn, để chứng minh hệ thống *sẵn sàng* mở rộng chứ không bắt buộc làm hết.

## 7. Phân tích SWOT

**Strengths (Điểm mạnh)**
- Chủ đề khác biệt, đúng nhu cầu fintech.
- Độ khó kỹ thuật cao nhưng business dễ hiểu → dễ trình bày.
- Nền C# giúp học Spring nhanh.

**Weaknesses (Điểm yếu)**
- Event Sourcing có đường cong học tập dốc.
- Làm một mình, dễ over-engineer hoặc bỏ dở.
- 100% AI → rủi ro "slop" nếu không kiểm soát chất lượng.

**Opportunities (Cơ hội)**
- Fintech VN tuyển backend Java rất mạnh.
- Ít ứng viên dám làm Event Sourcing → nổi bật.
- Có thể viết blog/series về quá trình → tăng thương hiệu cá nhân.

**Threats (Rủi ro)**
- Phạm vi phình to → không bao giờ "xong".
- Sa đà tính năng thay vì đào sâu chất lượng.
- Nếu không *hiểu* code AI sinh ra → trượt khi bị hỏi sâu.

### Đối sách với rủi ro chí mạng (100% AI + không hiểu code)
> **Nguyên tắc bất di bất dịch:** Mọi dòng code AI sinh ra, bạn phải đọc và giải thích được *vì sao*. Nếu một quyết định không giải thích được trong phỏng vấn, nó là nợ kỹ thuật, không phải tài sản. Mỗi phase có một bước "self-review & explain" trong backlog.

## 8. Tiêu chí thành công (Definition of Success)

Dự án thành công khi:
1. Một kỹ sư lạ clone repo về, đọc docs, chạy được trong < 15 phút.
2. Bạn giải thích được mọi quyết định kiến trúc khi bị hỏi sâu.
3. Hàm kiểm tra toàn vẹn sổ cái luôn pass sau hàng nghìn giao dịch ngẫu nhiên (property-based test).
4. Hệ thống chịu được test concurrency: không bao giờ tạo ra số dư âm sai hoặc tiền "bốc hơi".
5. Có ít nhất một tính năng "wao" hoạt động end-to-end: time-travel hoặc rebuild-from-events.

## 9. Bước kế tiếp
Đọc `01-architecture.md` để hiểu kiến trúc tổng thể và lý do chọn modular monolith.
