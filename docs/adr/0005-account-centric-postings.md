# ADR-0005: Ghi sổ kép theo mô hình account-centric postings

## Trạng thái
Accepted

## Bối cảnh
Phase 2 hiện thực hóa ghi sổ kép (double-entry). Câu hỏi: mô hình hóa một lần di
chuyển tiền (nạp/rút/chuyển) thành event như thế nào?

Catalog event ban đầu trong `02-domain-and-business.md` mang tính *transaction-centric*:
`MoneyTransferred(fromId, toId, amount, txId)` là **một** event chứa cả hai tài khoản.
Nhưng dự án đặt nặng:
- Invariant "số dư CUSTOMER không âm" phải enforce chắc chắn.
- Optimistic concurrency theo **từng tài khoản** (nền tảng chống double-spend ở Phase 3).

Để enforce hai điều trên, mỗi tài khoản cần là một consistency boundary có version
riêng — tức event phải thuộc về *một* stream tài khoản, không thể là một event chung
hai tài khoản.

## Quyết định
Mô hình **account-centric**: đơn vị cơ bản là **posting** (`MoneyPosted`) gắn vào một
tài khoản, đúng với ubiquitous language ("Posting" = một vế debit/credit lên một account).

- Mỗi lần di chuyển tiền sinh **hai** `MoneyPosted` cùng `txId`: một DEBIT ở tài khoản
  nguồn, một CREDIT ở tài khoản đích. Hai vế ghi **atomic** trong cùng một DB transaction.
- Nạp = vault → khách; rút = khách → vault; chuyển = khách → khách. Cả ba quy về cùng
  một cơ chế `move(from, to, amount)`.
- Invariant không-âm kiểm tra trên `AccountAggregate` bị debit; `SYSTEM_VAULT` được âm.
- `SYSTEM_VAULT` khởi tạo bằng một posting GENESIS (vế tiền phát hành duy nhất không có
  đối ứng). Hệ quả: tổng số dư toàn hệ thống luôn = seedAmount → integrity check.

## Hệ quả
**Được:**
- Optimistic concurrency theo từng tài khoản dùng được ngay (UNIQUE aggregate_id+version).
- Invariant không-âm nằm đúng chỗ (aggregate sở hữu số dư).
- Integrity toàn cục đơn giản: SUM(mọi số dư) == seedAmount.
- Trung thành với ubiquitous language "Posting/Transaction".

**Mất / đánh đổi:**
- Lệch khỏi *tên* event trong catalog ban đầu (`MoneyDeposited/Withdrawn/Transferred`
  như các event riêng). Thay bằng một event `MoneyPosted` có `direction` + `movementType`.
  → Đã cập nhật ghi chú trong `02-domain-and-business.md` và `03-data-and-eventstore.md`.
- Mỗi giao dịch tạo 2 event (2 stream) thay vì 1 → nhiều event hơn, nhưng đó là bản chất
  của sổ kép.
- `SYSTEM_VAULT` trở thành điểm nóng (mọi nạp/rút đụng vault) → có thể gây nhiều
  ConcurrencyConflict dưới tải. Sẽ xử lý ở Phase 3 (retry) / Phase 6 (tối ưu).

## Phương án đã cân nhắc
- **Transaction-centric** (`MoneyTransferred` một event, balance project toàn cục) —
  loại: khớp catalog docs nguyên văn nhưng không có consistency boundary theo tài khoản,
  khiến enforce không-âm + optimistic concurrency phải thêm cơ chế khóa riêng, phức tạp
  và yếu hơn.
- **Một event generic không có nghĩa nghiệp vụ** — loại: `MoneyPosted` đã đủ generic mà
  vẫn mang `movementType` để đọc hiểu lịch sử.
