package com.ledger.account.fraud;

import jakarta.validation.constraints.NotBlank;

/** Body đóng băng thủ công: cần lý do để lưu vết kiểm toán. */
public record FreezeRequest(@NotBlank String reason) {
}
