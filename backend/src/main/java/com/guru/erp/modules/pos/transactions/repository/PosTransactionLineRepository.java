package com.guru.erp.modules.pos.transactions.repository;

import com.guru.erp.modules.pos.transactions.domain.PosTransactionLine;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link PosTransactionLine}, mostly navigated via the {@code PosTransaction} aggregate. */
public interface PosTransactionLineRepository extends JpaRepository<PosTransactionLine, Long> {

    Optional<PosTransactionLine> findByTransaction_PublicIdAndPublicId(String transactionPublicId, String publicId);
}
