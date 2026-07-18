package com.guru.erp.platform.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.erp.platform.id.Ulid;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enqueues domain events into the outbox in the caller's transaction
 * (ARCHITECTURE.md §2). Services call {@link #publish} after a state change;
 * an async relay (added later) drains and dispatches them to consumers
 * (GL / Inventory / CRM). MANDATORY propagation guarantees the event and the
 * change commit together or not at all.
 */
@Service
public class OutboxPublisher {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publish(String aggregateType, String aggregatePublicId, String eventType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
        OutboxEvent event = new OutboxEvent(
            Ulid.next(), aggregateType, aggregatePublicId, eventType, json,
            Instant.now(Clock.systemUTC()));
        return repository.save(event);
    }
}
