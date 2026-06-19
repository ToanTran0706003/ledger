package com.ledger.audit.query;

/**
 * Kết quả kiểm tra hash-chain của event store. {@code firstBrokenSeq} là global_seq của
 * event đầu tiên bị gãy chuỗi (hash không khớp hoặc không nối đúng event trước), null nếu nguyên vẹn.
 */
public record HashChainReport(boolean intact, long eventsChecked, Long firstBrokenSeq) {
}
