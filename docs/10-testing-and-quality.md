# 10 — Testing & Quality

> Trong domain tài chính, test không phải "nice to have" — nó là *bằng chứng* rằng tiền luôn đúng. Đây cũng là nơi đối phó trực tiếp với rủi ro "100% AI mà không chắc đúng".

## 1. Kim tự tháp test
```
        /\
       /  \   E2E (ít): luồng người dùng thật qua API/UI
      /----\
     /      \  Integration (vừa): có DB thật (Testcontainers), projection, concurrency
    /--------\
   /          \ Unit (nhiều): aggregate, invariant, domain logic thuần
  /____________\
```

## 2. Unit test — domain là trọng tâm
- Test aggregate: cho một chuỗi event → trạng thái đúng; cho một command → sinh đúng event hoặc bị từ chối.
- Test invariant: rút quá số dư bị từ chối; transaction lệch vế bị từ chối.
- Thuần in-memory, nhanh, nhiều.

**Phong cách "given-when-then" theo nghiệp vụ:**
```
given: tài khoản đã mở, đã nạp 1.000.000
when : rút 1.500.000
then : bị từ chối với InsufficientFunds, không sinh event
```

## 3. Integration test — đúng đắn thật trên hạ tầng thật
- **Testcontainers**: bật PostgreSQL thật trong test → event store, migration, projection chạy như production.
- Test vòng đời đầy đủ: command → event store → projector → read model → query.
- Test rebuild: xóa read model, replay, assert ra kết quả y hệt.
- Test snapshot: có/không snapshot ra cùng kết quả.

## 4. Property-based test — vũ khí cho tính đúng đắn tài chính
Dùng **jqwik**. Thay vì test vài ca cố định, sinh *hàng nghìn* kịch bản ngẫu nhiên và assert các bất biến luôn đúng:

**Invariant cần luôn giữ:**
1. Không tài khoản CUSTOMER nào có số dư âm.
2. Tổng số dư toàn hệ thống == hằng số khởi tạo (double-entry).
3. Mọi transaction cân vế (Σ debit = Σ credit).
4. Read model khớp với kết quả replay từ event store.

```
property: với mọi dãy giao dịch hợp lệ ngẫu nhiên (mở, nạp, rút, chuyển),
          sau mỗi bước, cả 4 invariant trên đều đúng.
```
→ Nếu một dãy phá vỡ invariant, jqwik tự *thu nhỏ* về ca lỗi tối thiểu để bạn debug. Đây là thứ rất ít portfolio có và cực kỳ thuyết phục.

## 5. Concurrency test — chống race thật
- Bắn nhiều thread cùng rút/chuyển trên *cùng* một tài khoản.
- Assert: không số dư âm sai, tổng tiền không đổi, số giao dịch thành công khớp logic.
- Xác minh cơ chế optimistic concurrency + retry hoạt động dưới tải đồng thời.

## 6. Idempotency & resilience test
- Gửi cùng Idempotency-Key 2 lần (kể cả song song) → đúng một hiệu lực, lần sau trả lại kết quả cũ.
- Mô phỏng lỗi giữa chừng (vd sau khi ghi event, trước khi projection) → outbox đảm bảo cuối cùng read model vẫn đúng (eventual consistency).

## 7. Contract & API test
- **REST Assured** kiểm tra hợp đồng API: mã trạng thái, schema response, lỗi đúng định dạng.
- (Tùy chọn) sinh tài liệu API từ test (Spring REST Docs) → docs luôn khớp thực tế.

## 8. Chất lượng code & tự động hóa
- **Spotless**: format tự động, không tranh cãi.
- **Checkstyle/PMD**: bắt anti-pattern.
- **Coverage** (JaCoCo): ưu tiên *chất lượng test hành vi* hơn con số phần trăm. Đặt ngưỡng tối thiểu cho module domain.
- **OWASP Dependency-Check**: quét lỗ hổng thư viện trong CI.
- **CI (GitHub Actions)**: mọi PR phải build + test + lint + scan xanh mới merge.

## 9. Định nghĩa "chất lượng doanh nghiệp" cho dự án này
Một tính năng coi là *xong* khi:
- [ ] Có unit test cho domain logic
- [ ] Có integration test cho luồng end-to-end (nếu áp dụng)
- [ ] Không phá vỡ property-based invariant
- [ ] Tài liệu/ADR cập nhật nếu có quyết định mới
- [ ] CI xanh
- [ ] Bạn *giải thích được* mọi quyết định trong đó

## 10. Liên hệ với rủi ro "100% AI"
Property-based test + concurrency test + integrity check là **lưới an toàn khách quan**: dù code do AI sinh, nếu nó phá vỡ một invariant tài chính, test sẽ bắt được. Kết hợp với kỷ luật "hiểu mọi dòng", đây là cách biến một dự án AI thành một dự án *đáng tin và bảo vệ được trong phỏng vấn*.

## 11. Bước kế tiếp
Quay lại `08-todo-backlog.md` và bắt đầu Phase 0. Chúc thi công thuận lợi.
