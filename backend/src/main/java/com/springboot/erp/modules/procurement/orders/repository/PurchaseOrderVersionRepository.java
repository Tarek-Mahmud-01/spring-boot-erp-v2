package com.springboot.erp.modules.procurement.orders.repository;

import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrderVersion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link PurchaseOrderVersion} — the PO amendment history (FR-092). */
public interface PurchaseOrderVersionRepository extends JpaRepository<PurchaseOrderVersion, Long> {

    /** Version history for a PO, oldest first. */
    List<PurchaseOrderVersion> findByPurchaseOrderPublicIdOrderByVersionNoAsc(String poPublicId);
}
