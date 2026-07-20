package com.springboot.erp.modules.procurement.suppliers.repository;

import com.springboot.erp.modules.procurement.suppliers.domain.SupplierAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link SupplierAttachment}. */
public interface SupplierAttachmentRepository extends JpaRepository<SupplierAttachment, Long> {

    Optional<SupplierAttachment> findByPublicId(String publicId);

    /** Attachments for a supplier, newest first (reference {@code created_at desc}). */
    List<SupplierAttachment> findBySupplierIdOrderByCreatedAtDesc(String supplierId);

    Optional<SupplierAttachment> findByPublicIdAndSupplierId(String publicId, String supplierId);
}
