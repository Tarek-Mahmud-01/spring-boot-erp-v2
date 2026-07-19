package com.guru.erp.modules.finance.gl.repository;

import com.guru.erp.modules.finance.gl.domain.VoucherType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link VoucherType}. */
public interface VoucherTypeRepository extends JpaRepository<VoucherType, Long> {

    Optional<VoucherType> findByPublicId(String publicId);

    Optional<VoucherType> findByCode(String code);

    boolean existsByCode(String code);

    List<VoucherType> findByOperationalFalseOrderByCode();
}
