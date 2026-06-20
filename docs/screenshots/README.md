# Ảnh chụp giao diện (cho README)

> Tự động hoá không commit ảnh nhị phân được, nên đây là hướng dẫn để **bạn** chụp và bỏ vào thư
> mục này. Mỗi ảnh ~1 phút. Sau khi có ảnh, dán khối markdown ở cuối file vào `README.md`.

## Chuẩn bị
1. Chạy backend (`cd backend && ./gradlew bootRun`) và frontend (`cd frontend && npm run dev`).
2. Đăng nhập `admin` / `admin12345` (hoặc tự đăng ký) và tạo ít dữ liệu: mở 1 tài khoản VND + 1 USD,
   nạp tiền, quy đổi một ít để màn hình có nội dung đẹp.

## Các màn nên chụp (gợi ý tên file)

| Tên file | Màn / thao tác | Cho thấy điều gì |
|----------|----------------|------------------|
| `login.png` | Màn đăng nhập | Brand, dark theme, accent xanh |
| `dashboard.png` | Bảng điều khiển (có ≥2 tài khoản khác tiền tệ) | Tổng theo từng tiền tệ, badge "Sổ cân", thẻ tài khoản |
| `account.png` | Chi tiết tài khoản (có vài giao dịch) | **Replay "dựng lại số dư"**, biểu đồ + slider time-travel, sao kê |
| `fx.png` | Modal "Quy đổi tiền tệ" | Quy đổi đa tiền tệ, "tỉ giá do hệ thống" |
| `frozen.png` | Tài khoản bị đóng băng | Banner sắc lạnh + lý do, nút ghi nợ bị khoá |
| `audit.png` | Màn Kiểm toán | "Sổ cân", chênh lệch 0 |
| `admin.png` | Màn Quản trị (đăng nhập admin) | Hash-chain "Nguyên vẹn", danh sách đóng băng, duyệt giao dịch |

Để tạo trạng thái **đóng băng** cho `frozen.png`: nạp ≥ 200tr vào một tài khoản rồi rút 150tr
(luật "giao dịch lớn bất thường" sẽ tự đóng băng) — hoặc chạy `node ops/demo-e2e.mjs` (phần fraud).

## Markdown để dán vào README (sau khi có ảnh)

```markdown
| | |
|---|---|
| ![Bảng điều khiển](docs/screenshots/dashboard.png) | ![Chi tiết tài khoản](docs/screenshots/account.png) |
| ![Kiểm toán](docs/screenshots/audit.png) | ![Quản trị](docs/screenshots/admin.png) |
```

Gợi ý GIF: dùng [ScreenToGif](https://www.screentogif.com/) (Windows) quay ~10s thao tác
"mở tài khoản → nạp → xem replay dựng số dư" rồi lưu `docs/screenshots/replay.gif`.
