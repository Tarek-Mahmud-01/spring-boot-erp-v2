package com.guru.erp.modules.product.catalog.service;

import com.guru.erp.modules.product.catalog.domain.Product;
import com.guru.erp.modules.product.catalog.domain.ProductImage;
import com.guru.erp.modules.product.catalog.dto.ImageDtos.ImageCreateRequest;
import com.guru.erp.modules.product.catalog.dto.ImageDtos.ImageResponse;
import com.guru.erp.modules.product.catalog.mapper.ProductMapper;
import com.guru.erp.modules.product.catalog.repository.ProductImageRepository;
import com.guru.erp.modules.product.catalog.repository.ProductRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-011c ProductImage use-cases (FR-046). At most {@link #MAX_IMAGES} images
 * per product, each ≤ {@link #MAX_IMAGE_BYTES}. New images append at the next
 * position (0 = primary). Every mutation records an audit row.
 */
@Service
public class ImageService {

    public static final int MAX_IMAGES = 5;
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final String ENTITY = "product_image";

    private final ProductRepository products;
    private final ProductImageRepository images;
    private final ProductMapper mapper;
    private final AuditService auditService;

    public ImageService(ProductRepository products, ProductImageRepository images,
                        ProductMapper mapper, AuditService auditService) {
        this.products = products;
        this.images = images;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> list(String productPublicId) {
        Product p = loadProduct(productPublicId);
        return images.findByProductIdOrderByPosition(p.getId()).stream()
            .map(i -> mapper.toResponse(i, p.getPublicId()))
            .toList();
    }

    @Transactional
    public ImageResponse add(String productPublicId, ImageCreateRequest req) {
        Product p = loadProduct(productPublicId);
        if (req.bytes() > MAX_IMAGE_BYTES) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "Image exceeds the size limit",
                Map.of("maxBytes", MAX_IMAGE_BYTES));
        }
        long existing = images.countByProductId(p.getId());
        if (existing >= MAX_IMAGES) {
            throw new DomainException(ErrorCode.CONFLICT, "A product can have at most %d images"
                .formatted(MAX_IMAGES), Map.of("maxImages", MAX_IMAGES));
        }
        ProductImage img = new ProductImage();
        img.setProductId(p.getId());
        img.setFileId(req.fileId());
        img.setPosition((int) existing);
        img.setBytes(req.bytes());
        ProductImage saved = images.save(img);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.CREATE, null, snapshot(saved));
        return mapper.toResponse(saved, p.getPublicId());
    }

    @Transactional
    public void delete(String imagePublicId) {
        ProductImage img = images.findByPublicId(imagePublicId)
            .orElseThrow(() -> DomainException.notFound("ProductImage", imagePublicId));
        Map<String, Object> before = snapshot(img);
        images.delete(img);
        auditService.record(ENTITY, imagePublicId, AuditAction.DELETE, before, null);
    }

    private Product loadProduct(String publicId) {
        return products.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Product", publicId));
    }

    private Map<String, Object> snapshot(ProductImage i) {
        return Map.of("id", i.getPublicId(), "productId", i.getProductId(),
            "fileId", i.getFileId(), "position", i.getPosition(), "bytes", i.getBytes());
    }
}
