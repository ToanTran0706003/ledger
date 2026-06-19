package com.ledger.account.standingorder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandingOrderRepository extends JpaRepository<StandingOrder, UUID> {

    List<StandingOrder> findByActiveTrueAndNextRunAtLessThanEqual(Instant now);

    List<StandingOrder> findByOwnerUserId(String ownerUserId);
}
