# ADR-0004: Double-Entry với SYSTEM_VAULT để mô hình nguồn tiền

## Trạng thái
Accepted

## Bối cảnh
Một sổ cái phải trả lời được câu hỏi "tiền từ đâu ra, đi đâu" một cách nhất quán và
kiểm tra được. Nếu mô hình nạp tiền chỉ đơn giản là "+= amount" vào số dư khách thì
tiền *tự sinh ra* — không có invariant toàn cục, không kiểm tra được tính đúng đắn.

Ràng buộc:
- Phải đúng nguyên lý tài chính thật để gây ấn tượng với fintech.
- Muốn một invariant toàn cục có thể kiểm tra tự động (integrity check, property-based test).

## Quyết định
Áp dụng **double-entry bookkeeping**: mọi giao dịch có hai vế (debit + credit) với tổng
hai vế luôn bằng nhau — tiền chỉ *di chuyển* giữa các tài khoản, không tự sinh/mất.

Đưa vào một tài khoản hệ thống **`SYSTEM_VAULT`** (két ngân hàng) khởi tạo số dư lớn:
- **Nạp tiền** = chuyển `SYSTEM_VAULT` → tài khoản khách.
- **Rút tiền** = chuyển tài khoản khách → `SYSTEM_VAULT`.
- **Chuyển tiền** = tài khoản khách A → tài khoản khách B.

Hệ quả: **tổng số dư toàn hệ thống luôn là hằng số** = một invariant toàn cục.

## Hệ quả
**Được:**
- Invariant kiểm tra được: endpoint `/audit/integrity` assert `SUM(mọi số dư) == hằng số`;
  lệch dù 1 đơn vị → có bug nghiêm trọng. Property-based test ném hàng nghìn giao dịch
  ngẫu nhiên rồi assert invariant này — thứ hiếm thấy ở portfolio.
- Mô hình đúng nghiệp vụ tài chính thật, "tiền từ đâu ra" được giải triệt để.
- Mọi command (Deposit/Withdraw/Transfer) quy về cùng một cơ chế posting cân vế.

**Mất / đánh đổi:**
- `SYSTEM_VAULT` **được phép âm** (đại diện tiền đã phát hành ra lưu thông) — invariant
  "không âm" chỉ áp cho tài khoản CUSTOMER, không áp cho vault. Phải phân biệt loại tài
  khoản rõ ràng (CUSTOMER / SYSTEM_VAULT / SAVINGS).
- Mỗi giao dịch sinh tối thiểu hai posting → nhiều bút toán hơn mô hình ngây thơ, nhưng
  đó chính là cái giá đúng đắn của sổ kép.

## Phương án đã cân nhắc
- **Cập nhật số dư trực tiếp (single-entry, "+= / -=")** — loại: tiền tự sinh/mất, không
  có invariant toàn cục, không phản ánh nghiệp vụ tài chính thật.
- **Double-entry nhưng không có tài khoản vault** (nạp tiền tạo credit không đối ứng) —
  loại: phá vỡ tính cân vế, lại rơi vào "tiền từ đâu ra" không kiểm tra được.
