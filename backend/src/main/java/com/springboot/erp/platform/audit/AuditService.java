package com.springboot.erp.platform.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.web.RequestIdFilter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes one hash-chained audit row per mutation (ARCHITECTURE.md §2). Services
 * call {@link #record} inside their own transaction so the audit row commits
 * atomically with the change. The row hash chains off the previous row's hash,
 * making the log tamper-evident.
 */
@Service
public class AuditService {

    private static final String GENESIS = "0".repeat(64);

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final CurrentUser currentUser;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper, CurrentUser currentUser) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.currentUser = currentUser;
    }

    /**
     * Append an audit row. Runs in the caller's transaction (MANDATORY) so it
     * cannot be committed independently of the mutation it records.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog record(String entityType, String entityPublicId, AuditAction action,
                           Object before, Object after) {
        AuditLog head = repository.findTopByOrderByIdDesc();
        String prevHash = head == null ? GENESIS : head.getRowHash();

        String actor = currentUser.optional().map(p -> p.userPublicId()).orElse("system");
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        String beforeJson = toJson(before);
        String afterJson = toJson(after);
        Instant now = Instant.now(Clock.systemUTC());

        String rowHash = sha256(String.join("|",
            prevHash,
            entityType,
            String.valueOf(entityPublicId),
            action.name(),
            String.valueOf(actor),
            String.valueOf(beforeJson),
            String.valueOf(afterJson),
            now.toString()));

        AuditLog row = new AuditLog(entityType, entityPublicId, action, actor, requestId,
            beforeJson, afterJson, prevHash, rowHash, now);
        return repository.save(row);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit payload", e);
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
