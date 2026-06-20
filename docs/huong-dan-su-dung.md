# Hướng dẫn sử dụng Ledger

Tài liệu này dành cho người dùng cuối của ứng dụng Ledger: khách hàng sử dụng web app hằng ngày, quản trị viên xử lý kiểm soát rủi ro, và kiểm toán viên cần kiểm tra tính toàn vẹn của sổ cái.

Ledger là ứng dụng mô phỏng lõi sổ cái tài chính. Bạn có thể đăng ký tài khoản, mở tài khoản tiền, nạp/rút/chuyển tiền, quy đổi VND/USD, đặt giữ tiền, tạo lệnh chuyển định kỳ, bật xác thực 2 lớp, và theo dõi trạng thái sổ cái qua giao diện web.

> [Ảnh minh họa: Trang tổng quan Ledger sau khi đăng nhập]

## Mục lục

1. [Truy cập ứng dụng](#truy-cập-ứng-dụng)
2. [Khái niệm cần biết](#khái-niệm-cần-biết)
3. [Bắt đầu nhanh](#bắt-đầu-nhanh)
4. [Đăng ký, đăng nhập và 2FA khi đăng nhập](#đăng-ký-đăng-nhập-và-2fa-khi-đăng-nhập)
5. [Bảng điều khiển](#bảng-điều-khiển)
6. [Chi tiết tài khoản](#chi-tiết-tài-khoản)
7. [Nạp, rút, chuyển tiền và quy đổi](#nạp-rút-chuyển-tiền-và-quy-đổi)
8. [Đặt giữ tiền](#đặt-giữ-tiền)
9. [Lệnh chuyển tiền định kỳ](#lệnh-chuyển-tiền-định-kỳ)
10. [Kiểm toán sổ cái](#kiểm-toán-sổ-cái)
11. [Bảo mật và xác thực 2 lớp](#bảo-mật-và-xác-thực-2-lớp)
12. [Dành cho quản trị viên và kiểm toán viên](#dành-cho-quản-trị-viên-và-kiểm-toán-viên)
13. [Câu hỏi thường gặp và xử lý lỗi](#câu-hỏi-thường-gặp-và-xử-lý-lỗi)
14. [Ghi chú cho người dùng kỹ thuật](#ghi-chú-cho-người-dùng-kỹ-thuật)

## Truy cập ứng dụng

Khi hệ thống đã được chạy ở môi trường phát triển, bạn truy cập:

| Thành phần | Địa chỉ |
|------------|---------|
| Web UI | `http://localhost:5173` |
| API | `http://localhost:8080` |
| API qua gateway, nếu hệ thống bật gateway | `http://localhost:8090/api/core/...` |

Tài khoản demo quản trị trong môi trường phát triển:

| Vai trò | Username | Mật khẩu |
|---------|----------|----------|
| ADMIN | `admin` | `admin12345` |

Tài khoản demo trên chỉ dùng cho môi trường dev. Người dùng thường không dùng tài khoản admin, mà tự đăng ký tài khoản riêng trên màn Đăng nhập / Đăng ký.

## Khái niệm cần biết

### Người dùng và vai trò

Ledger có các nhóm người dùng chính:

| Nhóm | Có thể làm gì |
|------|---------------|
| Khách hàng | Đăng ký, đăng nhập, xem tài khoản của mình, nạp/rút/chuyển tiền, quy đổi, đặt giữ, tạo lệnh định kỳ, bật/tắt 2FA |
| ADMIN | Quản trị hệ thống, mở băng tài khoản, duyệt/từ chối giao dịch chờ, xử lý đảo giao dịch khi cần sửa sai |
| AUDITOR | Truy cập chức năng kiểm tra, đặc biệt là xác minh hash-chain để xem sổ có dấu hiệu bị sửa đổi hay không |

Một số thao tác chỉ xuất hiện hoặc chỉ thực hiện được khi tài khoản đăng nhập có quyền phù hợp. Nếu bạn thấy thông báo "Không có quyền", hãy kiểm tra lại bạn đang dùng đúng tài khoản hay chưa.

### Loại tài khoản

Ledger hỗ trợ 2 loại tài khoản:

| Loại | Ý nghĩa |
|------|---------|
| `CUSTOMER` | Tài khoản khách hàng thông thường |
| `SAVINGS` | Tài khoản tiết kiệm, có tính lãi |

Khi mở tài khoản, hãy chọn đúng loại tài khoản theo mục đích sử dụng. Tài khoản tiết kiệm phù hợp khi bạn muốn thể hiện nghiệp vụ có lãi; tài khoản khách hàng thông thường phù hợp cho nạp, rút, chuyển, giữ và quy đổi hằng ngày.

### Tiền tệ

Ledger hỗ trợ:

- `VND`
- `USD`

Số dư tổng quan được hiển thị theo từng tiền tệ. Ví dụ, VND và USD không cộng gộp thành một con số chung.

### Số dư và số dư khả dụng

Trong Ledger, bạn có thể thấy hai khái niệm:

| Khái niệm | Ý nghĩa |
|-----------|---------|
| Số dư | Tổng số tiền đang ghi nhận trên tài khoản |
| Số dư khả dụng | Phần có thể dùng để rút, chuyển hoặc đặt giữ tiếp |

Nếu có khoản đang bị đặt giữ, số dư khả dụng sẽ thấp hơn số dư. Ví dụ, tài khoản có số dư 10.000.000đ và đang giữ 2.000.000đ thì số dư khả dụng còn 8.000.000đ.

### Những quy tắc quan trọng

Ledger áp dụng một số quy tắc để bảo vệ sổ cái và người dùng:

| Quy tắc | Điều bạn cần biết |
|---------|-------------------|
| Số dư không âm | Tài khoản khách không được âm. Rút hoặc chuyển quá số dư khả dụng sẽ bị từ chối. |
| Đặt giữ | Đặt giữ làm giảm số dư khả dụng nhưng chưa trừ số dư. Khoản giữ có thể được nhả hoặc thu. Nếu hết hạn, hệ thống tự nhả. |
| Maker-checker | Chuyển tiền từ 100.000.000đ trở lên không chạy ngay. Giao dịch chuyển sang trạng thái chờ ADMIN khác duyệt. Người tạo không tự duyệt được. |
| Phát hiện gian lận | Rút khoản lớn bất thường, ví dụ từ 100.000.000đ, hoặc có quá nhiều lệnh ghi nợ dồn dập có thể làm tài khoản tự đóng băng. |
| Tài khoản đóng băng | Khi bị đóng băng, tài khoản vẫn nạp được nhưng không rút/chuyển cho tới khi ADMIN mở băng. |
| Hạn mức ngày | Tổng rút và chuyển trong một ngày của một tài khoản tối đa 500.000.000đ. |
| 2FA | Khi bật xác thực 2 lớp, mỗi lần đăng nhập cần thêm mã 6 số đổi mỗi 30 giây. |
| Toàn vẹn sổ cái | Giao dịch được ghi sổ kép, không sửa/xóa. Nếu cần sửa sai, ADMIN dùng bút toán bù để đảo giao dịch. |

## Bắt đầu nhanh

Phần này giúp bạn đi từ tài khoản mới đến giao dịch đầu tiên.

### 1. Mở Web UI

1. Mở trình duyệt.
2. Truy cập `http://localhost:5173`.
3. Chờ màn Đăng nhập / Đăng ký xuất hiện.

> [Ảnh minh họa: Màn Đăng nhập / Đăng ký]

### 2. Đăng ký tài khoản người dùng

1. Chọn phần Đăng ký.
2. Nhập username dài ít nhất 3 ký tự.
3. Nhập mật khẩu dài ít nhất 8 ký tự.
4. Gửi biểu mẫu đăng ký.
5. Sau khi đăng ký thành công, dùng thông tin vừa tạo để đăng nhập.

Người dùng thông thường nên tự đăng ký. Không dùng tài khoản `admin` cho các thao tác khách hàng hằng ngày.

### 3. Đăng nhập

1. Nhập username.
2. Nhập mật khẩu.
3. Chọn Đăng nhập.
4. Nếu tài khoản đã bật 2FA, nhập thêm mã 6 số từ ứng dụng xác thực.

Sau khi đăng nhập thành công, bạn sẽ vào Bảng điều khiển.

### 4. Mở tài khoản tiền

1. Trên Bảng điều khiển, chọn thao tác mở hoặc tạo tài khoản.
2. Chọn loại tài khoản: `CUSTOMER` hoặc `SAVINGS`.
3. Chọn tiền tệ: `VND` hoặc `USD`.
4. Xác nhận tạo tài khoản.
5. Kiểm tra thẻ tài khoản mới xuất hiện trên Bảng điều khiển.

Nếu bạn cần thử các nghiệp vụ chuyển tiền, hãy tạo ít nhất hai tài khoản phù hợp.

### 5. Nạp tiền lần đầu

1. Mở thẻ tài khoản vừa tạo.
2. Chọn Nạp.
3. Nhập số tiền.
4. Xác nhận.
5. Kiểm tra số dư sau khi giao dịch hoàn tất.

Nạp tiền là thao tác được phép ngay cả khi tài khoản đang bị đóng băng.

### 6. Thử chuyển tiền hoặc quy đổi

1. Mở tài khoản nguồn.
2. Chọn Chuyển nếu chuyển cùng tiền tệ.
3. Chọn Quy đổi nếu chuyển giữa VND và USD.
4. Nhập tài khoản nhận và số tiền theo biểu mẫu.
5. Xác nhận và kiểm tra kết quả.

Với quy đổi, tỉ giá do hệ thống đặt. Người dùng không tự nhập tỉ giá.

## Đăng ký, đăng nhập và 2FA khi đăng nhập

Màn Đăng nhập / Đăng ký là nơi bạn tạo tài khoản người dùng và truy cập hệ thống.

### Đăng ký tài khoản mới

Các bước:

1. Mở `http://localhost:5173`.
2. Chọn Đăng ký.
3. Nhập username.
4. Đảm bảo username có ít nhất 3 ký tự.
5. Nhập mật khẩu.
6. Đảm bảo mật khẩu có ít nhất 8 ký tự.
7. Gửi biểu mẫu.
8. Khi đăng ký thành công, quay lại phần Đăng nhập.

Lưu ý:

- Username là thông tin nhận diện khi đăng nhập.
- Mật khẩu nên đủ dài và không dùng lại từ nơi khác.
- Nếu nhập username hoặc mật khẩu không đạt yêu cầu, hệ thống sẽ từ chối và yêu cầu sửa.

### Đăng nhập khi chưa bật 2FA

Các bước:

1. Nhập username.
2. Nhập mật khẩu.
3. Chọn Đăng nhập.
4. Nếu thông tin đúng, hệ thống đưa bạn vào Bảng điều khiển.

### Đăng nhập khi đã bật 2FA

Khi đã bật 2FA, đăng nhập có thêm một bước xác thực.

Các bước:

1. Nhập username và mật khẩu.
2. Chọn Đăng nhập.
3. Khi hệ thống hỏi mã 6 số, mở ứng dụng xác thực như Google Authenticator hoặc Authy.
4. Lấy mã hiện tại của Ledger.
5. Nhập mã 6 số vào màn hình đăng nhập.
6. Xác nhận để hoàn tất đăng nhập.

Mã 2FA thay đổi mỗi 30 giây. Nếu mã vừa hết hạn, hãy chờ mã mới trong ứng dụng xác thực rồi nhập lại.

## Bảng điều khiển

Bảng điều khiển là màn hình tổng quan sau khi đăng nhập.

> [Ảnh minh họa: Bảng điều khiển với tổng số dư theo VND/USD và các thẻ tài khoản]

Bạn có thể dùng màn này để:

- Xem tổng số dư theo từng tiền tệ.
- Xem huy hiệu "Sổ cân".
- Xem danh sách thẻ tài khoản.
- Nhận biết tài khoản đang bị đóng băng qua chip ❄.
- Xem hoạt động gần đây.
- Mở chi tiết một tài khoản.

### Đọc tổng số dư theo tiền tệ

Ledger không cộng VND và USD vào một con số chung. Mỗi tiền tệ có tổng riêng để bạn tránh hiểu nhầm.

Ví dụ:

| Tiền tệ | Cách hiểu |
|---------|-----------|
| VND | Tổng số dư các tài khoản VND |
| USD | Tổng số dư các tài khoản USD |

Nếu bạn vừa quy đổi, hãy kiểm tra cả hai tổng để xem tiền đã giảm ở tài khoản nguồn và tăng ở tài khoản đích đúng tiền tệ.

### Huy hiệu "Sổ cân"

Huy hiệu "Sổ cân" cho biết hệ thống đang cân về mặt sổ cái. Ý nghĩa thực tế: tổng số dư mọi tài khoản khớp với lượng phát hành trong hệ thống, và chênh lệch là 0.

Nếu huy hiệu cho biết sổ không cân, hãy báo cho ADMIN hoặc người vận hành hệ thống. Người dùng thường không nên tự xử lý lỗi toàn vẹn sổ cái.

### Danh sách thẻ tài khoản

Mỗi thẻ tài khoản thường thể hiện:

- Loại tài khoản.
- Tiền tệ.
- Số dư.
- Trạng thái đặc biệt nếu có.

Nếu tài khoản bị đóng băng, thẻ sẽ có chip ❄. Khi đó bạn vẫn có thể mở chi tiết để xem sao kê, nhưng không thể rút/chuyển cho tới khi tài khoản được mở băng.

### Hoạt động gần đây

Khu vực hoạt động gần đây giúp bạn kiểm tra nhanh các giao dịch mới phát sinh. Nếu cần xem đầy đủ hơn, hãy mở Chi tiết tài khoản để xem sao kê dạng sổ cái.

## Chi tiết tài khoản

Màn Chi tiết tài khoản là nơi bạn xem sâu một tài khoản và thực hiện các thao tác chính.

> [Ảnh minh họa: Màn chi tiết tài khoản với biểu đồ, thanh time-travel và sao kê]

Màn này có:

- Số dư được dựng lại bằng replay từ chuỗi sự kiện.
- Biểu đồ biến động số dư.
- Thanh trượt time-travel để xem số dư tại thời điểm quá khứ.
- Sao kê dạng sổ cái.
- Các nút: Nạp, Rút, Chuyển, Đặt giữ, Quy đổi.

### Xem số dư hiện tại

Khi mở chi tiết tài khoản, số dư hiện tại được hiển thị từ dữ liệu sổ cái. Ledger dùng cơ chế replay, nghĩa là số dư được dựng lại từ chuỗi sự kiện thay vì chỉ tin vào một dòng dữ liệu có thể bị sửa.

Điều này giúp bạn hiểu vì sao sao kê và số dư luôn liên hệ với nhau: mỗi thay đổi số dư đều đến từ một giao dịch hoặc sự kiện ghi sổ.

### Dùng time-travel để xem số dư quá khứ

Time-travel giúp bạn trả lời câu hỏi: "Tại thời điểm đó, tài khoản có số dư bao nhiêu?"

Các bước:

1. Mở Chi tiết tài khoản.
2. Tìm biểu đồ và thanh trượt time-travel.
3. Kéo thanh trượt tới thời điểm muốn xem.
4. Quan sát số dư hiển thị tại thời điểm đó.
5. So sánh với sao kê để hiểu giao dịch nào làm số dư thay đổi.

Time-travel chỉ dùng để xem lại. Nó không sửa dữ liệu và không quay ngược giao dịch.

### Đọc sao kê dạng sổ cái

Sao kê dạng sổ cái liệt kê các bút toán/giao dịch đã xảy ra. Khi đọc sao kê, hãy chú ý:

- Thời điểm giao dịch.
- Loại giao dịch: nạp, rút, chuyển, lãi, thu giữ chỗ, quy đổi hoặc bút toán liên quan.
- Số tiền.
- Chiều ảnh hưởng tới tài khoản.

Vì Ledger không sửa/xóa giao dịch đã ghi, nếu có sai sót thì cách xử lý đúng là tạo bút toán bù. Với người dùng thông thường, hãy liên hệ ADMIN khi cần kiểm tra hoặc xử lý giao dịch sai.

## Nạp, rút, chuyển tiền và quy đổi

Các thao tác tiền nằm chủ yếu ở màn Chi tiết tài khoản và màn Chuyển tiền / Quy đổi.

### Nạp tiền

Nạp tiền làm tăng số dư tài khoản.

Các bước:

1. Mở Chi tiết tài khoản.
2. Chọn Nạp.
3. Nhập số tiền cần nạp.
4. Kiểm tra lại tiền tệ của tài khoản.
5. Xác nhận.
6. Kiểm tra số dư và hoạt động gần đây.

Lưu ý:

- Nạp tiền vẫn được phép khi tài khoản đang đóng băng.
- Nếu bạn nạp vào tài khoản VND, số dư VND tăng. Nếu nạp vào tài khoản USD, số dư USD tăng.

### Rút tiền

Rút tiền làm giảm số dư tài khoản.

Các bước:

1. Mở Chi tiết tài khoản.
2. Chọn Rút.
3. Nhập số tiền muốn rút.
4. Kiểm tra số dư khả dụng.
5. Xác nhận.
6. Chờ hệ thống phản hồi.

Rút tiền có thể bị từ chối nếu:

- Số dư khả dụng không đủ.
- Tài khoản đang bị đóng băng.
- Tổng rút và chuyển trong ngày vượt 500.000.000đ.
- Giao dịch bị hệ thống phát hiện là rủi ro theo luật gian lận.

Nếu rút khoản lớn bất thường, ví dụ từ 100.000.000đ, tài khoản có thể tự đóng băng sau giao dịch theo cơ chế phát hiện gian lận.

### Chuyển tiền cùng tiền tệ

Chuyển tiền dùng khi tài khoản nguồn và tài khoản nhận cùng tiền tệ.

Các bước:

1. Mở màn Chuyển tiền hoặc chọn Chuyển từ Chi tiết tài khoản.
2. Chọn tài khoản nguồn.
3. Nhập hoặc chọn tài khoản nhận.
4. Nhập số tiền.
5. Kiểm tra lại tiền tệ. Chuyển cùng tiền tệ không phải là quy đổi.
6. Xác nhận.
7. Xem thông báo kết quả.

Kết quả có thể là:

| Kết quả | Ý nghĩa |
|---------|---------|
| Thực hiện ngay | Giao dịch hợp lệ và dưới ngưỡng cần duyệt |
| Chờ duyệt | Giao dịch từ 100.000.000đ trở lên, cần ADMIN khác duyệt |
| Bị từ chối | Không đủ số dư khả dụng, bị đóng băng, vượt hạn mức ngày, không có quyền hoặc lỗi hợp lệ khác |

### Quy đổi giữa VND và USD

Quy đổi dùng khi bạn chuyển giá trị giữa hai tiền tệ khác nhau, ví dụ từ VND sang USD hoặc từ USD sang VND.

Các bước:

1. Mở Chi tiết tài khoản nguồn.
2. Chọn Quy đổi.
3. Chọn tài khoản đích khác tiền tệ.
4. Nhập số tiền nguồn.
5. Kiểm tra thông tin quy đổi mà hệ thống hiển thị.
6. Xác nhận.
7. Kiểm tra lại số dư của cả hai tài khoản.

Điểm quan trọng:

- Tỉ giá do hệ thống đặt.
- Người dùng không tự nhập tỉ giá.
- Quy đổi vẫn phải giữ sổ cân theo từng tiền tệ.

### Giao dịch lớn và trạng thái chờ duyệt

Với chuyển tiền từ 100.000.000đ trở lên, Ledger áp dụng maker-checker, còn gọi là nguyên tắc bốn-mắt.

Quy trình:

1. Người dùng tạo giao dịch chuyển tiền lớn.
2. Hệ thống không thực hiện ngay.
3. UI báo giao dịch "chờ duyệt".
4. Một ADMIN khác người tạo vào màn Quản trị.
5. ADMIN duyệt hoặc từ chối.
6. Nếu được duyệt, giao dịch mới được thực hiện.

Người tạo giao dịch không tự duyệt được. Đây là cơ chế kiểm soát rủi ro, đặc biệt với khoản tiền lớn.

## Đặt giữ tiền

Đặt giữ, hay hold, là cách giữ tạm một khoản tiền để không thể dùng cho giao dịch khác, nhưng chưa trừ thật khỏi số dư.

> [Ảnh minh họa: Khu vực đặt giữ trong Chi tiết tài khoản]

### Khi nào dùng đặt giữ

Bạn dùng đặt giữ khi muốn khóa tạm một khoản, ví dụ để chờ xác nhận trước khi thu thật. Trong thời gian khoản tiền bị giữ:

- Số dư vẫn hiển thị như cũ.
- Số dư khả dụng giảm.
- Khoản bị giữ không thể dùng để rút hoặc chuyển.

### Tạo khoản đặt giữ

Các bước:

1. Mở Chi tiết tài khoản.
2. Chọn Đặt giữ.
3. Nhập số tiền cần giữ.
4. Kiểm tra số dư khả dụng.
5. Xác nhận.
6. Kiểm tra khu vực đang giữ nếu màn hình hiển thị.

Nếu số dư khả dụng không đủ, hệ thống sẽ từ chối.

### Nhả khoản giữ

Nhả khoản giữ dùng khi bạn không cần giữ khoản tiền đó nữa.

Các bước:

1. Mở Chi tiết tài khoản.
2. Tìm danh sách khoản đang giữ.
3. Chọn khoản muốn nhả.
4. Chọn Nhả.
5. Xác nhận.
6. Kiểm tra số dư khả dụng tăng trở lại.

Nhả khoản giữ không làm tăng số dư, vì khoản tiền chưa từng bị trừ khỏi số dư. Nó chỉ đưa khoản đang giữ trở lại phần khả dụng.

### Thu khoản giữ

Thu khoản giữ dùng khi khoản giữ cần được trừ thật.

Các bước:

1. Mở Chi tiết tài khoản.
2. Tìm khoản đang giữ.
3. Chọn Thu.
4. Xác nhận.
5. Kiểm tra số dư giảm và sao kê có dòng ghi nhận tương ứng.

Sau khi thu, khoản tiền mới thật sự bị trừ khỏi số dư.

### Khoản giữ hết hạn

Khoản giữ có thể tự nhả khi hết hạn. Khi đó:

- Số dư không đổi.
- Số dư khả dụng tăng lại.
- Khoản giữ không còn chiếm phần khả dụng.

Nếu bạn thấy số dư khả dụng thay đổi mà không có giao dịch rút/chuyển mới, hãy kiểm tra danh sách hold hoặc sao kê để xem khoản giữ có vừa được nhả hay không.

## Lệnh chuyển tiền định kỳ

Màn Định kỳ dùng để tạo lệnh chuyển tiền tự động lặp lại theo chu kỳ.

> [Ảnh minh họa: Màn Định kỳ với danh sách lệnh chuyển tự động]

### Tạo lệnh định kỳ

Các bước:

1. Vào màn Định kỳ.
2. Chọn tạo lệnh định kỳ mới.
3. Chọn tài khoản nguồn.
4. Nhập hoặc chọn tài khoản nhận.
5. Nhập số tiền.
6. Chọn chu kỳ lặp lại theo lựa chọn trên giao diện.
7. Kiểm tra lại thông tin.
8. Xác nhận tạo lệnh.

Khi đến kỳ, hệ thống tự tạo giao dịch chuyển tiền theo thông tin đã cấu hình.

### Theo dõi lệnh định kỳ

Trong màn Định kỳ, bạn có thể xem danh sách lệnh đã tạo. Khi kiểm tra, hãy đối chiếu:

- Tài khoản nguồn.
- Tài khoản nhận.
- Số tiền.
- Chu kỳ lặp.
- Trạng thái hoặc lịch sử thực hiện nếu giao diện hiển thị.

### Lệnh định kỳ có thể bị từ chối khi chạy

Một lệnh định kỳ không đảm bảo luôn thành công. Khi đến kỳ, giao dịch vẫn phải đi qua các quy tắc nghiệp vụ như giao dịch thường:

- Số dư khả dụng phải đủ.
- Tài khoản nguồn không bị đóng băng.
- Không vượt hạn mức ngày 500.000.000đ.
- Nếu khoản chuyển từ 100.000.000đ trở lên, giao dịch có thể chuyển sang trạng thái chờ duyệt theo maker-checker.

Nếu bạn không thấy lệnh định kỳ tạo ra giao dịch như mong đợi, hãy kiểm tra số dư khả dụng, trạng thái đóng băng và thông báo lỗi liên quan.

## Kiểm toán sổ cái

Màn Kiểm toán dùng để xem trạng thái "Sổ cân".

> [Ảnh minh họa: Màn Kiểm toán hiển thị chênh lệch 0]

### "Sổ cân" nghĩa là gì

Ledger ghi sổ kép. Mỗi giao dịch tiền phải có các bút toán tương ứng để tiền không tự sinh ra hoặc biến mất.

Khi màn Kiểm toán báo:

| Trạng thái | Ý nghĩa |
|------------|---------|
| Sổ cân | Tổng số dư mọi tài khoản bằng lượng phát hành, chênh lệch 0 |
| Không cân | Có dấu hiệu sai lệch cần kiểm tra |

Người dùng thường chỉ cần hiểu trạng thái này như một đèn kiểm tra sức khỏe của sổ cái. Nếu có sai lệch, hãy báo ADMIN hoặc người phụ trách vận hành.

### Cách kiểm tra

1. Vào màn Kiểm toán.
2. Xem trạng thái "Sổ cân".
3. Kiểm tra chênh lệch nếu giao diện hiển thị.
4. Nếu chênh lệch khác 0, không tự tạo giao dịch để "sửa". Hãy chuyển cho ADMIN xử lý.

### Vì sao không sửa/xóa giao dịch

Ledger không sửa hoặc xóa giao dịch đã ghi. Nếu cần sửa sai, ADMIN xử lý bằng bút toán bù, còn gọi là reverse. Cách này giữ lịch sử minh bạch: giao dịch sai vẫn còn trong sổ, và giao dịch bù thể hiện việc điều chỉnh.

## Bảo mật và xác thực 2 lớp

Màn Bảo mật dùng để bật hoặc tắt 2FA bằng TOTP.

TOTP là mã xác thực 6 số đổi mỗi 30 giây. Bạn có thể dùng ứng dụng xác thực như:

- Google Authenticator.
- Authy.

> [Ảnh minh họa: Màn Bảo mật với mã QR hoặc khóa bí mật 2FA]

### Trước khi bật 2FA

Hãy chuẩn bị:

1. Điện thoại hoặc thiết bị có cài ứng dụng xác thực.
2. Tài khoản Ledger đang đăng nhập.
3. Thời gian trên thiết bị tương đối chính xác, vì mã TOTP phụ thuộc vào thời gian.

### Bật 2FA

Các bước:

1. Vào màn Bảo mật.
2. Chọn bật 2FA.
3. Hệ thống hiển thị mã QR hoặc khóa bí mật.
4. Mở Google Authenticator hoặc Authy.
5. Thêm tài khoản mới trong ứng dụng xác thực.
6. Quét mã QR. Nếu không quét được, nhập khóa bí mật thủ công.
7. Ứng dụng xác thực sẽ hiển thị mã 6 số cho Ledger.
8. Nhập mã 6 số đó vào Ledger để xác nhận bật 2FA.
9. Khi xác nhận thành công, các lần đăng nhập sau sẽ yêu cầu mã 6 số.

Lưu ý:

- Không chia sẻ khóa bí mật hoặc mã 6 số cho người khác.
- Nếu mã 6 số bị từ chối, hãy chờ mã mới rồi nhập lại.
- Mã cũ có thể hết hạn sau 30 giây.

### Đăng nhập sau khi bật 2FA

Các bước:

1. Nhập username và mật khẩu như bình thường.
2. Khi Ledger yêu cầu mã xác thực 2 lớp, mở ứng dụng xác thực.
3. Nhập mã 6 số hiện tại.
4. Hoàn tất đăng nhập.

Nếu mất thiết bị xác thực, hãy liên hệ quản trị viên hoặc người vận hành môi trường demo để được hỗ trợ theo quy trình nội bộ.

### Tắt 2FA

Các bước:

1. Đăng nhập vào Ledger.
2. Vào màn Bảo mật.
3. Chọn tắt 2FA.
4. Mở ứng dụng xác thực.
5. Nhập mã 6 số hiện tại để xác nhận.
6. Sau khi tắt thành công, lần đăng nhập sau không hỏi mã 2FA nữa.

Nếu bạn dùng máy tính chung hoặc tài khoản có quyền quản trị, nên bật 2FA.

## Dành cho quản trị viên và kiểm toán viên

Màn Quản trị chỉ dành cho tài khoản có vai trò phù hợp, như ADMIN hoặc AUDITOR. Giao diện có thể hiển thị khác nhau tùy quyền; server vẫn là nơi quyết định bạn được phép thao tác gì.

> [Ảnh minh họa: Màn Quản trị với hash-chain, tài khoản đóng băng và giao dịch chờ duyệt]

### Đăng nhập bằng tài khoản ADMIN demo

Trong môi trường phát triển:

1. Mở `http://localhost:5173`.
2. Đăng nhập với username `admin`.
3. Nhập mật khẩu `admin12345`.
4. Vào màn Quản trị.

Không dùng tài khoản demo này trong môi trường thật.

### Xác minh hash-chain

Hash-chain giúp phát hiện sổ có bị giả mạo hay không. Đây là kiểm tra toàn vẹn ở mức chuỗi sự kiện.

Các bước:

1. Đăng nhập bằng tài khoản ADMIN hoặc AUDITOR.
2. Vào màn Quản trị.
3. Chọn chức năng xác minh hash-chain.
4. Chờ hệ thống kiểm tra.
5. Đọc kết quả:
   - Nguyên vẹn: chưa phát hiện dấu hiệu sửa đổi.
   - Phát hiện sửa đổi: cần dừng xử lý nghiệp vụ và điều tra.

Nếu phát hiện sửa đổi, không cố "sửa tay" dữ liệu. Hãy giữ nguyên hiện trạng để điều tra.

### Mở băng tài khoản

Tài khoản có thể tự đóng băng khi hệ thống phát hiện rủi ro, ví dụ rút khoản lớn bất thường hoặc quá nhiều lệnh ghi nợ dồn dập.

Chỉ ADMIN mới nên mở băng.

Các bước:

1. Đăng nhập bằng tài khoản ADMIN.
2. Vào màn Quản trị.
3. Tìm danh sách tài khoản đang đóng băng.
4. Xem lý do đóng băng nếu giao diện hiển thị.
5. Kiểm tra với người dùng hoặc dữ liệu liên quan trước khi mở.
6. Chọn Mở băng.
7. Xác nhận thao tác.
8. Báo người dùng thử lại rút/chuyển nếu phù hợp.

Sau khi mở băng, tài khoản có thể rút/chuyển trở lại, miễn là vẫn thỏa các quy tắc khác như số dư khả dụng và hạn mức ngày.

### Duyệt hoặc từ chối giao dịch chờ

Giao dịch chuyển tiền từ 100.000.000đ trở lên cần maker-checker.

Nguyên tắc quan trọng:

- Giao dịch lớn không thực hiện ngay.
- Một ADMIN khác người tạo phải duyệt.
- Người tạo không tự duyệt được.
- ADMIN có thể duyệt hoặc từ chối.

Các bước:

1. Đăng nhập bằng tài khoản ADMIN.
2. Vào màn Quản trị.
3. Mở danh sách giao dịch chờ duyệt.
4. Kiểm tra người tạo, tài khoản nguồn, tài khoản nhận, số tiền và thời điểm.
5. Nếu hợp lệ, chọn Duyệt.
6. Nếu không hợp lệ hoặc cần kiểm tra thêm, chọn Từ chối.
7. Thông báo cho người tạo giao dịch nếu cần.

Nếu bạn là người tạo giao dịch, hãy nhờ ADMIN khác xử lý. Hệ thống sẽ không cho bạn tự duyệt.

### Đảo giao dịch bằng bút toán bù

Ledger không sửa hoặc xóa giao dịch đã ghi. Khi cần sửa sai, ADMIN dùng reverse, tức tạo bút toán bù.

Khi sử dụng reverse:

1. Xác định đúng giao dịch cần đảo.
2. Kiểm tra lý do nghiệp vụ.
3. Thực hiện đảo giao dịch theo quyền ADMIN.
4. Kiểm tra sao kê sau khi đảo.

Reverse không làm giao dịch cũ biến mất. Nó tạo thêm một giao dịch bù để lịch sử vẫn minh bạch.

## Câu hỏi thường gặp và xử lý lỗi

### "Số dư không đủ"

Ý nghĩa: số dư khả dụng không đủ để thực hiện rút, chuyển hoặc đặt giữ.

Cách xử lý:

1. Mở Chi tiết tài khoản.
2. Kiểm tra số dư khả dụng, không chỉ số dư.
3. Xem có khoản hold nào đang giữ tiền không.
4. Giảm số tiền giao dịch hoặc nhả hold nếu phù hợp.
5. Nếu vẫn sai, báo ADMIN kiểm tra sao kê.

### "Tài khoản đã bị đóng băng"

Ý nghĩa: tài khoản đang ở trạng thái bị đóng băng vì lý do rủi ro hoặc quản trị.

Cách xử lý:

1. Bạn vẫn có thể nạp tiền.
2. Bạn không thể rút hoặc chuyển tiền cho tới khi được mở băng.
3. Liên hệ ADMIN.
4. ADMIN kiểm tra lý do và mở băng nếu hợp lệ.

### "Vượt hạn mức ngày"

Ý nghĩa: tổng rút và chuyển trong ngày của tài khoản đã vượt hoặc sẽ vượt 500.000.000đ.

Cách xử lý:

1. Giảm số tiền giao dịch.
2. Chia giao dịch sang ngày khác nếu phù hợp.
3. Chờ sang ngày tiếp theo rồi thử lại.

### "Cần mã xác thực 2 lớp"

Ý nghĩa: tài khoản của bạn đã bật 2FA.

Cách xử lý:

1. Mở Google Authenticator hoặc Authy.
2. Tìm mã của Ledger.
3. Nhập mã 6 số hiện tại.
4. Nếu mã hết hạn, chờ mã mới rồi nhập lại.

### "Giao dịch chờ duyệt"

Ý nghĩa: giao dịch chuyển tiền lớn, từ 100.000.000đ trở lên, đã được tạo nhưng chưa thực hiện.

Cách xử lý:

1. Chờ ADMIN khác người tạo duyệt.
2. Theo dõi trạng thái trên giao diện nếu có.
3. Không tạo lại nhiều lần nếu không cần, để tránh trùng yêu cầu.
4. Nếu cần gấp, liên hệ ADMIN.

### "Không có quyền"

Ý nghĩa: tài khoản đăng nhập không được phép thực hiện thao tác đó.

Các trường hợp thường gặp:

- Bạn đang thao tác trên tài khoản không thuộc về mình.
- Chức năng chỉ dành cho ADMIN.
- Chức năng chỉ dành cho ADMIN hoặc AUDITOR.
- Mở băng, duyệt giao dịch, đảo giao dịch là thao tác quản trị.

Cách xử lý:

1. Kiểm tra bạn đã đăng nhập đúng tài khoản chưa.
2. Nếu thao tác thuộc quyền quản trị, dùng tài khoản ADMIN phù hợp.
3. Nếu bạn là khách hàng, liên hệ ADMIN thay vì thử lại nhiều lần.

### "Quá nhiều yêu cầu"

Ý nghĩa: bạn thao tác quá nhanh hoặc gửi quá nhiều yêu cầu trong thời gian ngắn. Hệ thống giới hạn tần suất để chống lạm dụng.

Cách xử lý:

1. Dừng thao tác trong vài phút.
2. Thử lại sau.
3. Nếu đang đăng nhập sai mật khẩu nhiều lần, kiểm tra lại thông tin trước khi thử tiếp.

### Chuyển tiền không chạy ngay

Nguyên nhân thường gặp:

- Số tiền từ 100.000.000đ trở lên nên giao dịch đang chờ maker-checker.
- Tài khoản bị đóng băng.
- Vượt hạn mức ngày.
- Số dư khả dụng không đủ.

Cách xử lý:

1. Xem thông báo trên màn hình.
2. Nếu chờ duyệt, liên hệ ADMIN.
3. Nếu bị từ chối, chỉnh số tiền hoặc xử lý trạng thái tài khoản.

### Số dư và sao kê không giống điều tôi nghĩ

Hãy kiểm tra:

1. Bạn đang xem đúng tài khoản chưa.
2. Bạn đang xem đúng tiền tệ chưa.
3. Có khoản hold nào đang làm giảm số dư khả dụng không.
4. Thanh time-travel có đang ở thời điểm quá khứ không.
5. Có giao dịch định kỳ nào vừa chạy không.
6. Có giao dịch quy đổi nào ảnh hưởng đồng thời VND và USD không.

Nếu vẫn chưa rõ, hãy gửi ảnh màn hình và thời điểm giao dịch cho ADMIN để kiểm tra.

## Ghi chú cho người dùng kỹ thuật

Ledger có Web UI tại `http://localhost:5173` và API tại `http://localhost:8080`. Nếu hệ thống chạy qua gateway, đường dẫn API core có dạng `http://localhost:8090/api/core/...`.

Tài liệu này tập trung vào cách dùng qua giao diện web. Không nên gọi API trực tiếp nếu bạn chỉ đang thao tác như người dùng cuối, vì UI đã áp dụng đúng luồng đăng nhập, phân quyền, 2FA, kiểm soát trạng thái và thông báo lỗi thân thiện hơn.

