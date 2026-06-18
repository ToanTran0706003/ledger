package com.ledger.account.api;

import com.ledger.account.projection.ReadModelRebuildService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thao tác vận hành read model. Phase 1: rebuild từ event store.
 * (Phase 5 sẽ giới hạn endpoint này cho vai trò ADMIN.)
 */
@RestController
@RequestMapping("/admin/read-model")
public class AdminReadModelController {

    private final ReadModelRebuildService rebuild;

    public AdminReadModelController(ReadModelRebuildService rebuild) {
        this.rebuild = rebuild;
    }

    @PostMapping("/rebuild")
    public ResponseEntity<Void> rebuild() {
        rebuild.rebuild();
        return ResponseEntity.noContent().build();
    }
}
