# ADR-0014: Hash-chain chống giả mạo cho event store

## Trạng thái
Accepted

## Bối cảnh
Event store là nguồn sự thật append-only (ADR-0002). `GET /audit/integrity` chứng minh sổ
luôn cân, nhưng không phát hiện việc một dòng event đã ghi bị sửa. Kiểm toán fintech cần
bằng chứng **bất biến**: nếu ai đó sửa lịch sử, hệ thống phải phát hiện được. Đây cũng là dịp
phô diễn đúng sức mạnh của event sourcing (chuỗi sự kiện bất biến).

## Quyết định
- Mỗi event mang `hash = SHA-256(prev_hash + aggregate_id + ':' + version + ':' + event_type
  + ':' + payload + ':' + metadata)`, nối chuỗi **theo từng aggregate** (`prev_hash` = hash của
  event trước trong cùng aggregate; event đầu nối vào GENESIS = 64 số 0).
- **Chuỗi theo aggregate, KHÔNG toàn cục** — tận dụng optimistic concurrency đã serialize ghi
  trong một aggregate; nhờ vậy các aggregate khác nhau vẫn ghi **song song** (giữ throughput,
  ADR-0010). Chuỗi toàn cục buộc serialize MỌI lần ghi → loại.
- **Tính hash bằng SQL** trên dạng canonical jsonb của Postgres (`payload::text`): write và verify
  cho ra cùng một chuỗi byte, **không phụ thuộc cách Java serialize** (tránh lệch do scale của
  BigDecimal / thứ tự khoá JSON). Dùng `sha256()` built-in (PostgreSQL 11+) nên **không cần
  pgcrypto**. Biểu thức hash khớp y hệt ở ba nơi: append (`JdbcEventStore`), backfill (V12), verify
  (`HashChainVerifier`) — cặp runtime append↔verify được test `intact_chain_verifies_clean` canh.
- **metadata (userId/ip/correlationId) nằm trong hash** — bảo vệ cả "ai/từ đâu", không chỉ nội dung.
- Migration V12 thêm cột `prev_hash`/`hash` và **backfill** các event cũ theo thứ tự chuỗi.
- `GET /audit/hash-chain` (ADMIN/AUDITOR): tái tính hash + kiểm tra liên kết bằng window function
  (`lag(hash)`), trả về `{intact, eventsChecked, firstBrokenSeq}`.

## Threat model (trung thực — phạm vi bảo vệ)
**Phát hiện được:**
- Sửa tại chỗ bất kỳ trường nào của một event đã ghi (payload, event_type, version, metadata)
  → hash tái tính lệch.
- Xoá một event **giữa** chuỗi → `prev_hash` của event kế không khớp hash event trước nữa
  (đã có test `deleting_a_middle_event...`).

**KHÔNG phát hiện (giới hạn có chủ đích, ghi rõ để không overclaim):**
- Xoá event **mới nhất** của một aggregate, hoặc xoá **cả** stream — chưa có "neo" đầu chuỗi
  ngoài DB. Giảm thiểu: kỷ luật append-only + kiểm soát truy cập DB. Hướng nâng cấp: sổ đăng ký
  head theo aggregate, hoặc neo ra ngoài (notarization/timestamping).
- Giả mạo bởi người **có quyền ghi và biết cách dựng hash** — SHA-256 không khoá là **tamper-
  EVIDENCE**, không phải **tamper-proof**. Hướng nâng cấp: HMAC với secret key (cần pgcrypto hoặc
  tính ở Java), hoặc neo ngoài.
- `verify()` quét **toàn bảng** (chưa phân trang) — đủ ở quy mô hiện tại; lớn lên cần verify
  tăng dần theo cursor.

## Hệ quả
**Được:** bằng chứng chống giả mạo rẻ, gắn chặt với lõi ES; giữ nguyên khả năng ghi song song;
không thêm dependency (sha256 built-in). Trung thực về giới hạn → đáng tin với người kiểm toán.
**Mất / đánh đổi:** thêm 1 SELECT (đọc hash đầu chuỗi) mỗi lần append — point-lookup có index,
không đáng kể; biểu thức hash lặp ở 3 nơi (append dùng bind param, verify/backfill dùng tên cột —
khác cấu trúc nên không gộp được; canh bằng test + chú thích chéo).

## Phương án đã cân nhắc
- **Chuỗi toàn cục theo global_seq** — loại: phải serialize mọi lần ghi, phá throughput.
- **Tính hash ở Java (MessageDigest)** — loại: roundtrip jsonb có thể đổi scale BigDecimal/thứ tự
  khoá → verify lệch; tính trong SQL trên `payload::text` tránh hẳn.
- **HMAC có secret ngay từ đầu** — hoãn: cần quản lý khoá + pgcrypto; ghi vào hướng nâng cấp.
