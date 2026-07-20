package com.springboot.erp.platform.entity;

import com.springboot.erp.platform.id.Ulid;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Cross-cutting base for every persistent entity (ARCHITECTURE.md §2).
 *
 * <ul>
 *   <li>{@code id} — internal bigint surrogate key (FK target, never exposed).</li>
 *   <li>{@code publicId} — app-generated ULID char(26), the external identifier.</li>
 *   <li>Auditing columns — created/updated at + by, filled by JPA auditing.</li>
 *   <li>{@code deletedAt} — soft delete; {@link SQLRestriction} hides deleted rows
 *       from every query automatically.</li>
 *   <li>{@code version} — optimistic lock, mandatory on every entity.</li>
 * </ul>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at is null")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 26, columnDefinition = "char(26)", unique = true)
    private String publicId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by", updatable = false, length = 26, columnDefinition = "char(26)")
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "updated_by", length = 26, columnDefinition = "char(26)")
    private String updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @PrePersist
    void assignPublicId() {
        if (publicId == null) {
            publicId = Ulid.next();
        }
    }

    /** Mark this row soft-deleted. Callers must still emit an audit + check refs. */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public long getVersion() {
        return version;
    }
}
