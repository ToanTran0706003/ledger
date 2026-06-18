package com.ledger.audit.api;

import com.ledger.audit.query.IntegrityReport;
import com.ledger.audit.query.IntegrityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final IntegrityService integrityService;

    public AuditController(IntegrityService integrityService) {
        this.integrityService = integrityService;
    }

    @GetMapping("/integrity")
    public IntegrityReport integrity() {
        return integrityService.check();
    }
}
