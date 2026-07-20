package com.springboot.erp.modules.product.catalog.service;

import com.springboot.erp.modules.product.catalog.domain.LifecycleState;
import com.springboot.erp.modules.product.catalog.domain.Product;
import com.springboot.erp.modules.product.catalog.dto.LifecycleDtos.LifecycleTransitionRequest;
import com.springboot.erp.modules.product.catalog.dto.ProductDtos.ProductResponse;
import com.springboot.erp.modules.product.catalog.mapper.ProductMapper;
import com.springboot.erp.modules.product.catalog.repository.ProductRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-070/074 — product lifecycle transitions. The allowed moves live in
 * {@link LifecycleState#MACHINE}; activation (→ ACTIVE) additionally requires a
 * completeness check (name + sell price). Records an audit row per transition.
 */
@Service
public class ProductLifecycleService {

    private static final String ENTITY = "product";

    private final ProductRepository products;
    private final ProductMapper mapper;
    private final AuditService auditService;

    public ProductLifecycleService(ProductRepository products, ProductMapper mapper,
                                   AuditService auditService) {
        this.products = products;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional
    public ProductResponse transition(String publicId, LifecycleTransitionRequest req) {
        Product p = products.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Product", publicId));
        LifecycleState from = p.getLifecycleState();
        LifecycleState to = LifecycleState.MACHINE.transition(from, req.toState());
        if (to == LifecycleState.ACTIVE) {
            String missing = missingForActive(p);
            if (missing != null) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "Cannot activate: missing " + missing, Map.of("missing", missing));
            }
        }
        Map<String, Object> before = Map.of("lifecycleState", from.name());
        p.setLifecycleState(to);
        Product saved = products.save(p);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            Map.of("lifecycleState", to.name(), "reason", req.reason() == null ? "" : req.reason()));
        return mapper.toResponse(saved);
    }

    private static String missingForActive(Product p) {
        if (p.getName() == null || p.getName().isBlank()) {
            return "name";
        }
        if (p.getSell() == null) {
            return "sell_price";
        }
        return null;
    }
}
