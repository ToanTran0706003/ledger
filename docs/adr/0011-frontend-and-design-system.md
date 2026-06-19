# ADR-0011: Frontend & design system (anti-slop)

## Trạng thái
Accepted

## Bối cảnh
Phase 7 cần một giao diện *có chủ đích*, chống "AI slop" (doc 06), trình diễn được bản
chất event sourcing và tạo cảm giác tin cậy cho domain tài chính.

## Quyết định
**Tech:** React + TypeScript + Vite (SPA gọi REST API). Vite (không Next) vì không cần
SSR cho một dashboard gọi API; build nhẹ, chạy một lệnh (`npm run dev`). Không thêm
thư viện UI/CSS framework — tự dựng **design token system** bằng CSS variables (đúng
tinh thần doc 06: token có chủ đích, không mặc định).

**Áp dụng skill `Leonxlnx/taste-skill` (anti-slop) + doc 06:**
- Một theme tối duy nhất; nền zinc trung tính + **một** accent xanh (dưới 80% sat); **một**
  hệ bo góc; số tiền dùng **monospace tabular** (ngôn ngữ sổ cái: số xếp cột).
- **Typography có cá tính** (áp dụng plugin frontend-design của Anthropic — tránh font hệ
  thống generic): cặp **Space Grotesk** (chữ) + **JetBrains Mono** (số), nạp qua Google Fonts
  có fallback hệ thống. **Chiều sâu thị giác** tiết chế: quầng sáng teal mờ ở đỉnh trang +
  card có gradient/viền sáng đỉnh (không dùng gradient tím generic).
- **Signature element**: "dựng lại số dư từ chuỗi sự kiện" — replay từng posting cộng dồn
  thành số dư (cho thấy event sourcing). Tôn trọng `prefers-reduced-motion`.
- Sao kê dạng **sổ cái thật**: cột Thời gian / Loại / Ghi nợ / Ghi có / Số dư sau.
- Đủ trạng thái: loading (skeleton), empty (lời mời mở tài khoản), error (toast nói rõ,
  surface message tiếng Việt từ backend).
- Accessibility: WCAG AA (tương phản nút/chữ), focus nhìn thấy, nhãn ARIA, responsive,
  `min-h-[100dvh]`. **Không em-dash** trong copy. Không "hero 3 card generic", không glow tím AI.
- Auth: JWT lưu localStorage; gắn Bearer + tự sinh Idempotency-Key cho thao tác ghi.

**Bộ màn hình (5):** Đăng nhập/Đăng ký · Bảng điều khiển (chỉ số tổng quan + huy hiệu
"Sổ cân" integrity + thẻ tài khoản + feed hoạt động) · Chi tiết tài khoản (replay +
**biểu đồ vùng số dư theo thời gian** dựng bằng SVG tự viết + **slider time-travel** kéo
được, kiểm chứng lại bằng API máy chủ + sao kê) · Chuyển tiền · Kiểm toán (khoe invariant
double-entry: tổng số dư == lượng phát hành). `/audit/integrity` cho phép mọi user đăng
nhập xem (chỉ số minh bạch); các endpoint /audit khác vẫn cần ADMIN/AUDITOR.

**Backend bổ sung:** `GET /accounts` liệt kê tài khoản của người dùng đang đăng nhập
(cho dashboard) — ownership theo `owner = userId`.

## Hệ quả
**Được:**
- UI có cá tính, không generic; trình diễn đúng "luận điểm" của dự án (event → số dư).
- Build nhẹ, chạy/độc lập backend; CI build cả frontend.
- Trải nghiệm tin cậy: số rõ ràng, lỗi hướng dẫn được, lịch sử truy được.

**Mất / đánh đổi:**
- SPA không SEO/SSR (không cần cho app sau đăng nhập).
- Token lưu localStorage (đơn giản; cân nhắc httpOnly cookie nếu cần chống XSS chặt hơn).
- Tự dựng CSS thay vì dùng component lib → nhiều CSS tay hơn, đổi lại kiểm soát thẩm mỹ.

## Phương án đã cân nhắc
- **Next.js** — hoãn: SSR/route phức tạp không cần cho dashboard gọi API.
- **Tailwind/Component library (MUI...)** — loại: dễ ra look mặc định "AI"; token tự dựng
  thể hiện chủ đích thiết kế tốt hơn cho mục tiêu anti-slop.
