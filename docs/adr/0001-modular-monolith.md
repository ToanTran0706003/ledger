# ADR-0001: Modular Monolith thay vì Microservices

## Trạng thái
Accepted

## Bối cảnh
Ledger là dự án portfolio do một người phát triển, mục tiêu thể hiện chiều sâu kỹ
thuật (Event Sourcing + CQRS, double-entry, concurrency) cho nhà tuyển dụng fintech.
Câu hỏi nền tảng: triển khai ngay theo microservices, hay bắt đầu bằng monolith?

Ràng buộc:
- Một người làm, budget $0, không có đội DevOps.
- Phải "chuẩn doanh nghiệp" nhưng vẫn ra tính năng đều và hiểu được mọi quyết định.
- Giá trị nằm ở domain (sổ cái đúng, đồng thời, audit), không ở hạ tầng phân tán.

## Quyết định
Bắt đầu bằng **Modular Monolith**: một artifact triển khai duy nhất, chia thành các
module theo nghiệp vụ (`account`, `ledger`, `audit`, `iam`) với một `shared` kernel.
Module chỉ giao tiếp qua *public interface* hoặc *domain event*, không gọi vào internal
của nhau — đây là điều kiện để sau này tách thành service mà không phải viết lại.

Việc lên microservices được hoãn tới Phase 9 (tùy chọn), chỉ làm khi có lý do trình
diễn rõ ràng.

## Hệ quả
**Được:**
- Độ phức tạp DevOps thấp (1 artifact, không service discovery / network giữa service).
- Tốc độ ra tính năng nhanh; transaction trong một process — đơn giản hóa nhất quán.
- Ranh giới module sạch vẫn thể hiện tư duy hệ thống.

**Mất / đánh đổi:**
- Phải tự kỷ luật giữ ranh giới module (dễ bị "xuyên biên giới" nếu cẩu thả).
- Không có sẵn câu chuyện "scale độc lập từng service" — bù lại bằng Phase 9.

**Để giữ đường nâng cấp mở:**
- Cấm import chéo internal giữa module (cân nhắc ArchUnit để ép buộc).
- Giao tiếp giữa module qua event/interface ngay từ đầu, kể cả khi cùng process.

## Phương án đã cân nhắc
- **Microservices ngay từ đầu** — loại: với một người làm, dễ sa lầy vào
  Docker/K8s/networking thay vì chiều sâu domain. Một modular monolith *sạch* khó và
  ấn tượng hơn một microservices *lộn xộn*.
- **Monolith truyền thống (package by layer)** — loại: ranh giới mờ, khó tách sau này,
  không thể hiện được tư duy thiết kế module.
