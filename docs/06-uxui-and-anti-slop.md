# 06 — UX/UI & Anti-Slop

> Vấn đề: dự án 100% AI dễ rơi vào "AI slop" — giao diện generic, code mặc định, quyết định không có chủ đích. Chương này đặt ra kỷ luật để mọi thứ trông *có chủ ý*, do một kỹ sư/designer thật sự cân nhắc.

## 1. "Slop" là gì và vì sao phải tránh

**Slop = sản phẩm trông như mọi sản phẩm AI khác:** cùng một bố cục, cùng palette, cùng cách diễn đạt, không có điểm nhận diện, không có lý do đằng sau lựa chọn. Nhà tuyển dụng tinh ý nhận ra ngay → mất điểm.

### Dấu hiệu nhận biết slop (để tránh)
- UI: nền cream + serif tương phản + accent cam đất; hoặc nền đen + một màu neon; bố cục báo giấy hairline. Ba "look" này là *mặc định AI*, dùng vì lười chứ không vì hợp đề bài.
- Copy: "Empower your finances", "Seamless experience", "Powerful and intuitive" — sáo rỗng.
- Code: comment thừa kiểu `// increment i`, tên biến generic, lặp khuôn mẫu không cần thiết.
- Quyết định: không giải thích được *vì sao* chọn → đó là slop.

## 2. Nguyên tắc thiết kế (Design Principles)

1. **Mọi lựa chọn có lý do.** Màu, font, bố cục đều phục vụ chủ đề (sổ cái tài chính: chính xác, tin cậy, minh bạch). Ghi lý do vào design notes.
2. **Hero là luận điểm.** Màn hình chính nên trình diễn thứ đặc trưng nhất của Ledger — không phải một card số dư generic, mà có thể là *dòng event chảy thành số dư* (cho thấy bản chất event sourcing).
3. **Typography mang cá tính.** Chọn cặp display/body có chủ đích, không phải font mặc định. Thiết lập type scale rõ ràng.
4. **Cấu trúc là thông tin.** Đánh số, nhãn, đường kẻ chỉ dùng khi *mã hóa điều gì đó thật* (vd: thứ tự event), không phải trang trí.
5. **Chuyển động có chủ đích.** Một khoảnh khắc được dàn dựng (vd: animation replay dựng số dư) đáng giá hơn hàng loạt hiệu ứng rải rác. Tôn trọng `prefers-reduced-motion`.
6. **Tiêu điểm một chỗ.** Một điểm nhấn đáng nhớ; phần còn lại giữ yên tĩnh, kỷ luật.

## 3. Định hướng thẩm mỹ cho Ledger (gợi ý, không bắt buộc)

Chủ đề là *sổ cái* — vật liệu của nó là **bút toán, dòng kẻ, con số xếp cột, dấu vết thời gian**. Một hướng có chủ đích có thể khai thác chính ngôn ngữ này (cột ghi nợ/ghi có, dòng chảy event, dấu thời gian) thay vì mượn look fintech generic. Khi bắt tay làm UI thật, hãy lập một **design token system** nhỏ (4–6 màu đặt tên, 2–3 vai trò typography, một concept bố cục, một "signature element") và tự phản biện: phần nào nghe như mặc định AI thì làm lại.

> Tài liệu này không chốt cứng palette — việc chốt nên làm ở bước thiết kế thật, có phản biện, để tránh đúng cái bẫy slop đang nói tới.

## 4. UX cho domain tài chính (đặc thù)

- **Tin cậy là cảm giác cốt lõi.** Số tiền hiển thị rõ ràng, không mơ hồ; thao tác tiền luôn có xác nhận; trạng thái (đang xử lý/thành công/thất bại) minh bạch.
- **Không bao giờ để người dùng nghi ngờ tiền của họ.** Loading rõ ràng, không "nhảy số" gây hoang mang, lịch sử luôn truy được.
- **Thông báo lỗi hướng dẫn được.** "Số dư không đủ: hiện có 200.000₫, cần 500.000₫" — nói rõ vì sao và làm gì tiếp, bằng giọng của hệ thống, không xin lỗi sáo rỗng.
- **Empty state là lời mời hành động,** không phải khoảng trống.
- **Đối chiếu trực quan:** sao kê dạng cột giống sổ cái thật, có balance-after mỗi dòng.

## 5. Viết nội dung (copy) trong UI
- Gọi tên theo thứ người dùng điều khiển, không theo cách hệ thống xây. "Lịch sử giao dịch", không phải "Event stream" (với người dùng cuối).
- Động từ chủ động, nhất quán: nút "Chuyển tiền" → toast "Đã chuyển".
- Sentence case, không sáo rỗng, mỗi phần tử làm đúng một việc.

## 6. Khả năng tiếp cận (Accessibility) — sàn chất lượng bắt buộc
- Tương phản màu đạt WCAG AA.
- Điều hướng bàn phím đầy đủ, focus nhìn thấy được.
- Nhãn ARIA cho thành phần tương tác.
- Tôn trọng `prefers-reduced-motion`.
- Responsive xuống mobile.

## 7. Anti-slop cho CODE (không chỉ UI)

> Đây là phần sống còn vì dự án 100% AI.

1. **Đọc & hiểu mọi dòng.** Không commit code không giải thích được. Mỗi PR tự hỏi: "Bị hỏi sâu chỗ này, mình trả lời được không?"
2. **Không comment thừa.** Comment giải thích *vì sao*, không phải *cái gì* (code đã nói cái gì).
3. **Tên có nghĩa theo domain.** `reverseTransaction`, không phải `processData2`.
4. **Không khuôn mẫu vô cớ.** Không thêm abstraction/factory/interface khi chưa có nhu cầu thật (YAGNI).
5. **Nhất quán phong cách.** Formatter + linter ép buộc (Spotless/Checkstyle), không tranh cãi tay.
6. **Mỗi quyết định lớn → ADR.** Người đọc sau hiểu *vì sao*.
7. **Test thể hiện ý định.** Tên test mô tả hành vi nghiệp vụ, không phải tên hàm.

## 8. Quy trình tự phản biện (áp cho cả UI lẫn code)
Trước khi coi một phần là "xong", hỏi:
- Nếu một người khác làm cùng đề bài bằng AI, họ có ra *giống hệt* không? Nếu có → có thể đang slop, cân nhắc làm khác có chủ đích.
- Có chi tiết nào mình *không* giải thích được lý do không? Sửa hoặc bỏ.
- "Bỏ bớt một món phụ kiện": có gì thừa, trang trí không phục vụ mục đích → cắt.

## 9. Bước kế tiếp
Đọc `07-roadmap-and-phases.md`.
