package com.ledger.shared.domain;

/**
 * Sự thật đã xảy ra, bất biến. Đặt tên ở thì quá khứ (AccountOpened, MoneyTransferred).
 * Một event không thể "rút lại" — chỉ có thể bù bằng một event khác.
 */
public interface DomainEvent {

    /** Tên logic dùng làm event_type khi lưu trữ. Mặc định là tên class. */
    default String eventType() {
        return getClass().getSimpleName();
    }
}
