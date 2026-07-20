package com.springboot.erp.modules.crm.loyalty.repository;

import com.springboot.erp.modules.crm.loyalty.domain.CustomerTransaction;
import com.springboot.erp.modules.crm.loyalty.domain.TransactionType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for the {@link CustomerTransaction} purchase-history projection
 * (reference {@code list_history} / {@code record_pos_transaction}).
 */
public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, Long> {

    Optional<CustomerTransaction> findByPublicId(String publicId);

    /** Most-recent-first purchase history for one customer (reference FR-215). */
    Page<CustomerTransaction> findByCustomerIdOrderByOccurredAtDesc(String customerId, Pageable pageable);

    /** Idempotency lookup for the outbox-fed projection writer (reference {@code _existing_projection}). */
    Optional<CustomerTransaction> findByCustomerIdAndReceiptNumberAndType(
        String customerId, String receiptNumber, TransactionType type);
}
