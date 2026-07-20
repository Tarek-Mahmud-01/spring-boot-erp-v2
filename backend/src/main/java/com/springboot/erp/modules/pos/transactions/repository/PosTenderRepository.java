package com.springboot.erp.modules.pos.transactions.repository;

import com.springboot.erp.modules.pos.transactions.domain.PosTender;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link PosTender}, mostly navigated via the {@code PosTransaction} aggregate. */
public interface PosTenderRepository extends JpaRepository<PosTender, Long> {

    Optional<PosTender> findByTransaction_PublicIdAndPublicId(String transactionPublicId, String publicId);
}
