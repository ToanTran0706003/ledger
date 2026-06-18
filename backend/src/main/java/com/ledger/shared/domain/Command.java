package com.ledger.shared.domain;

/**
 * Ý định thay đổi trạng thái (OpenAccount, Deposit, Transfer). Khác event ở chỗ:
 * một command CÓ THỂ bị từ chối (vi phạm invariant), một event thì đã là sự thật.
 */
public interface Command {
}
