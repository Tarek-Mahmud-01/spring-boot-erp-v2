package com.springboot.erp.modules.product.catalog.service;

import com.springboot.erp.modules.product.catalog.domain.CustomAttributeValue;
import com.springboot.erp.modules.product.catalog.domain.Product;
import com.springboot.erp.modules.product.catalog.domain.ProductVariant;
import com.springboot.erp.modules.product.catalog.dto.CustomAttributeDtos.CustomAttributeResponse;
import com.springboot.erp.modules.product.catalog.dto.CustomAttributeDtos.CustomAttributeUpsertRequest;
import com.springboot.erp.modules.product.catalog.mapper.ProductMapper;
import com.springboot.erp.modules.product.catalog.repository.CustomAttributeValueRepository;
import com.springboot.erp.modules.product.catalog.repository.ProductRepository;
import com.springboot.erp.modules.product.catalog.repository.ProductVariantRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-012 CustomAttributeValue use-cases. Upsert-by-key: a (product,variant,
 * scope,key) tuple is unique, so re-posting the same key updates the value rather
 * than creating a duplicate. Every mutation records an audit row.
 */
@Service
public class CustomAttributeService {

    private static final String ENTITY = "product_custom_attribute_value";

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CustomAttributeValueRepository repository;
    private final ProductMapper mapper;
    private final AuditService auditService;

    public CustomAttributeService(ProductRepository products, ProductVariantRepository variants,
                                  CustomAttributeValueRepository repository, ProductMapper mapper,
                                  AuditService auditService) {
        this.products = products;
        this.variants = variants;
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CustomAttributeResponse> list(String productPublicId) {
        Product p = loadProduct(productPublicId);
        return repository.findByProductIdOrderById(p.getId()).stream()
            .map(a -> mapper.toResponse(a, p.getPublicId(), variantPublicId(a.getVariantId())))
            .toList();
    }

    @Transactional
    public CustomAttributeResponse upsert(String productPublicId, CustomAttributeUpsertRequest req) {
        Product p = loadProduct(productPublicId);
        String scope = req.scope() == null || req.scope().isBlank() ? "product" : req.scope();
        Long variantInternalId = null;
        if (req.variantId() != null) {
            ProductVariant v = variants.findByPublicId(req.variantId())
                .orElseThrow(() -> DomainException.notFound("ProductVariant", req.variantId()));
            if (!v.getParentProductId().equals(p.getId())) {
                throw DomainException.notFound("ProductVariant", req.variantId());
            }
            variantInternalId = v.getId();
        }
        CustomAttributeValue existing = repository
            .findByProductIdAndVariantIdAndScopeAndKey(p.getId(), variantInternalId, scope, req.key())
            .orElse(null);
        if (existing != null) {
            Map<String, Object> before = snapshot(existing);
            existing.setValue(req.value());
            CustomAttributeValue saved = repository.save(existing);
            auditService.record(ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, snapshot(saved));
            return mapper.toResponse(saved, p.getPublicId(), variantPublicId(saved.getVariantId()));
        }
        CustomAttributeValue row = new CustomAttributeValue();
        row.setProductId(p.getId());
        row.setVariantId(variantInternalId);
        row.setScope(scope);
        row.setKey(req.key());
        row.setValue(req.value());
        CustomAttributeValue saved = repository.save(row);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.CREATE, null, snapshot(saved));
        return mapper.toResponse(saved, p.getPublicId(), variantPublicId(saved.getVariantId()));
    }

    @Transactional
    public void delete(String attributePublicId) {
        CustomAttributeValue row = repository.findByPublicId(attributePublicId)
            .orElseThrow(() -> DomainException.notFound("CustomAttributeValue", attributePublicId));
        Map<String, Object> before = snapshot(row);
        repository.delete(row);
        auditService.record(ENTITY, attributePublicId, AuditAction.DELETE, before, null);
    }

    private String variantPublicId(Long variantInternalId) {
        if (variantInternalId == null) {
            return null;
        }
        return variants.findById(variantInternalId).map(ProductVariant::getPublicId).orElse(null);
    }

    private Product loadProduct(String publicId) {
        return products.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Product", publicId));
    }

    private Map<String, Object> snapshot(CustomAttributeValue a) {
        return Map.of(
            "id", a.getPublicId(),
            "productId", a.getProductId(),
            "scope", a.getScope(),
            "key", a.getKey(),
            "value", a.getValue() == null ? "" : a.getValue(),
            "version", a.getVersion());
    }
}
