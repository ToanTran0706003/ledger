package com.ledger.audit.web;

import com.ledger.audit.service.IntegrityService;
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
    public IntegrityResponse integrity() {
        return integrityService.integrity();
    }

    @GetMapping("/accounts/count")
    public CountResponse accountCount() {
        return new CountResponse(integrityService.accountCount());
    }
}
