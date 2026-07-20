package com.springboot.erp.platform.outbox;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /** Unprocessed events, oldest first — the relay drains these in order. */
    List<OutboxEvent> findByProcessedAtIsNullOrderByIdAsc(Limit limit);
}
