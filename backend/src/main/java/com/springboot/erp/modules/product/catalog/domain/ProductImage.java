package com.springboot.erp.modules.product.catalog.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * ENT-011c ProductImage — FR-046 (≤ 5 images, ≤ 5 MB each). Ordered by
 * {@code position} (0 = primary). {@code uq_product_images_position} keeps one
 * image per (product, position); {@code ck_product_images_bytes_non_negative}
 * guards the size. {@code productId} is an internal bigint FK (same slice).
 */
@Entity
@Table(
    name = "product_images",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_product_images_position", columnNames = {"product_id", "position"})
)
public class ProductImage extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "file_id", nullable = false, length = 80)
    private String fileId;

    @Column(name = "position", nullable = false)
    private int position = 0;

    @Column(name = "bytes", nullable = false)
    private long bytes = 0;

    public ProductImage() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }
}
