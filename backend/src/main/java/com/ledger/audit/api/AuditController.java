package com.ledger.audit.api;

import com.ledger.audit.query.HashChainReport;
import com.ledger.audit.query.HashChainVerifier;
import com.ledger.audit.query.IntegrityReport;
import com.ledger.audit.query.IntegrityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final IntegrityService integrityService;
    private final HashChainVerifier hashChainVerifier;

    public AuditController(IntegrityService integrityService, HashChainVerifier hashChainVerifier) {
        this.integrityService = integrityService;
        this.hashChainVerifier = hashChainVerifier;
    }

    @GetMapping("/integrity")
    public IntegrityReport integrity() {
        return integrityService.check();
    }

    /**
     * Kiểm tra hash-chain của event store (ADMIN/AUDITOR): phát hiện sửa đổi tại chỗ một event
     * đã ghi hoặc xoá event giữa chuỗi. Không thay thế kiểm soát truy cập DB — xem threat model
     * ở ADR-0014 (keyless SHA-256 là tamper-EVIDENCE, không phải tamper-proof).
     */
    @GetMapping("/hash-chain")
    public HashChainReport hashChain() {
        return hashChainVerifier.verify();
    }
}
