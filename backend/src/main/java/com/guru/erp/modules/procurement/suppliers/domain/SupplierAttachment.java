package com.guru.erp.modules.procurement.suppliers.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-026a SupplierAttachment — a supporting document (registration cert, agreement, …) linked to
 * a {@link Supplier} (reference {@code app.procurement.models.SupplierAttachment}).
 *
 * <p>The parent supplier is referenced by its ULID {@code public_id} ({@code supplier_id}) so the
 * DTO can expose it without leaking the bigint surrogate; the V40 migration still enforces a real
 * FK to {@code suppliers.public_id} with {@code on delete cascade} (same slice). Base columns come
 * from {@link BaseEntity}. Binary storage / media-URL rendering is deferred (see slice notes).
 */
@Entity
@Table(name = "supplier_attachments")
public class SupplierAttachment extends BaseEntity {

    /** ULID public_id of the owning supplier (same-slice FK enforced in V40). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "supplier_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String supplierId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    /** ULID public id of the uploading user (cross-slice, loose ref). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uploaded_by", length = 26, columnDefinition = "char(26)")
    private String uploadedBy;

    public SupplierAttachment() {
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
