package com.springboot.erp.modules.crm.loyalty.service;

import com.springboot.erp.modules.crm.loyalty.domain.CustomerTransaction;
import com.springboot.erp.modules.crm.loyalty.domain.TransactionType;
import com.springboot.erp.modules.crm.loyalty.dto.CustomerTransactionDtos.CustomerTransactionIngestRequest;
import com.springboot.erp.modules.crm.loyalty.dto.CustomerTransactionDtos.CustomerTransactionResponse;
import com.springboot.erp.modules.crm.loyalty.dto.CustomerTransactionDtos.TransactionLine;
import com.springboot.erp.modules.crm.loyalty.dto.CustomerTransactionDtos.TransactionPayment;
import com.springboot.erp.modules.crm.loyalty.mapper.LoyaltyMapper;
import com.springboot.erp.modules.crm.loyalty.repository.CustomerTransactionRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purchase-history projection use-cases (reference {@code ingest_transaction} /
 * {@code record_pos_transaction} / {@code list_history}, US-041 / FR-215-217).
 *
 * <p>In production this projection is populated by an outbox subscriber
 * reacting to {@code pos.sale.completed} / {@code pos.refund.created} (the
 * reference's {@code app.crm.fulfillment} handlers), never by a direct
 * cross-module call — the vertical-slice rule forbids POS hard-calling into
 * CRM. Until that subscriber is wired, {@link #ingest} is exposed directly so
 * purchase history is testable end-to-end; its idempotency key (customer,
 * receipt, type) is exactly what makes it safe to later drive from an
 * at-least-once outbox dispatcher without change.
 */
@Service
public class CustomerTransactionService {

    static final String AUDIT_ENTITY = "customer_transaction";

    private final CustomerTransactionRepository repository;
    private final LoyaltyMapper mapper;
    private final AuditService auditService;
    private final Clock clock = Clock.systemUTC();

    public CustomerTransactionService(CustomerTransactionRepository repository, LoyaltyMapper mapper,
                                      AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<CustomerTransactionResponse> listHistory(String customerId, Pageable pageable) {
        return repository.findByCustomerIdOrderByOccurredAtDesc(customerId, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerTransactionResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("CustomerTransaction", publicId)));
    }

    /**
     * Record a POS / Sales transaction projection (reference {@code ingest_transaction}
     * stand-in feed). Payment data must arrive already masked (FR-217); {@code #mask}
     * is applied defensively so a raw PAN never persists even if the caller slipped one in.
     */
    @Transactional
    public CustomerTransactionResponse ingest(String customerId, CustomerTransactionIngestRequest req) {
        CustomerTransaction t = new CustomerTransaction();
        t.setCustomerId(customerId);
        t.setReceiptNumber(req.receiptNumber());
        t.setType(req.type() != null ? req.type() : TransactionType.SALE);
        t.setOccurredAt(req.occurredAt() != null ? req.occurredAt() : Instant.now(clock));
        t.setTotalAmount(req.totalAmount());
        t.setSubtotalAmount(req.subtotalAmount());
        t.setTotalCurrency(req.totalCurrency().toUpperCase());
        t.setLocationId(req.locationId());
        t.setPaymentSummary(toPaymentMaps(req.paymentSummary()));
        t.setLines(toLineMaps(req.lines()));
        t.setRefundOfId(req.refundOfId());

        CustomerTransaction saved = repository.save(t);
        // Audit the create — a customer.manage-gated write of financial/PII purchase
        // history (masked PAN + line items). Masked PAN only; no raw card data.
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            Map.of("customerId", customerId, "receiptNumber", saved.getReceiptNumber(),
                "type", saved.getType(), "totalAmount", saved.getTotalAmount(),
                "totalCurrency", saved.getTotalCurrency()));
        return mapper.toResponse(saved);
    }

    /**
     * Idempotent outbox-fed projection writer (reference {@code record_pos_transaction}).
     * Returns the created-or-already-present row, or {@code null} when there is no CRM
     * customer attached (a walk-in sale) — both are no-ops an outbox dispatcher acknowledges.
     */
    @Transactional
    public CustomerTransactionResponse recordFromEvent(String customerId, String receiptNumber,
            TransactionType type, long totalAmount, Long subtotalAmount, String currency,
            Instant occurredAt, String locationId, List<TransactionLine> lines,
            List<TransactionPayment> paymentSummary, String refundOfId) {
        if (customerId == null || receiptNumber == null) {
            return null;
        }
        CustomerTransaction existing = repository
            .findByCustomerIdAndReceiptNumberAndType(customerId, receiptNumber, type)
            .orElse(null);
        if (existing != null) {
            return mapper.toResponse(existing);
        }

        CustomerTransaction t = new CustomerTransaction();
        t.setCustomerId(customerId);
        t.setReceiptNumber(receiptNumber);
        t.setType(type);
        t.setOccurredAt(occurredAt != null ? occurredAt : Instant.now(clock));
        t.setTotalAmount(totalAmount);
        t.setSubtotalAmount(subtotalAmount);
        t.setTotalCurrency((currency != null ? currency : "USD").toUpperCase());
        t.setLocationId(locationId);
        t.setPaymentSummary(toPaymentMaps(paymentSummary));
        t.setLines(toLineMaps(lines));
        t.setRefundOfId(refundOfId);
        CustomerTransaction saved = repository.save(t);
        return mapper.toResponse(saved);
    }

    private List<Map<String, Object>> toPaymentMaps(List<TransactionPayment> payments) {
        if (payments == null) {
            return List.of();
        }
        return payments.stream()
            .map((TransactionPayment p) -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("method", p.method());
                m.put("maskedPan", mask(p.maskedPan()));
                m.put("amount", p.amount());
                return (Map<String, Object>) m;
            })
            .toList();
    }

    private List<Map<String, Object>> toLineMaps(List<TransactionLine> lines) {
        if (lines == null) {
            return List.of();
        }
        return lines.stream()
            .map((TransactionLine ln) -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("sku", ln.sku());
                m.put("name", ln.name());
                m.put("qty", ln.qty());
                m.put("unitPriceAmount", ln.unitPriceAmount());
                m.put("lineAmount", ln.lineAmount());
                m.put("categoryId", ln.categoryId());
                return (Map<String, Object>) m;
            })
            .toList();
    }

    /** FR-217 — keep at most the last 4 digits. */
    private static String mask(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    }
}
