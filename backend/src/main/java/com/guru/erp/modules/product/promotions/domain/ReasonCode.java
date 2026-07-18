package com.guru.erp.modules.product.promotions.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Discount reason-code master — FR-068. A configurable lookup of selectable
 * reason codes; when a promotion's {@code reasonRequired} flag is set, or a
 * manual discount is applied at POS, the operator picks a code from this master
 * instead of free-typing.
 *
 * <p>Global (single-tenant). {@code code} is unique among live (non-deleted)
 * rows — enforced case-insensitively in the service so a soft-deleted code can
 * be re-introduced later. {@code code} is immutable after creation.
 *
 * <p>Domain columns only; id / publicId / audit / version / soft-delete come
 * from {@link BaseEntity}.
 */
@Entity
@Table(name = "reason_codes")
public class ReasonCode extends BaseEntity {

    @Column(name = "code", nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public ReasonCode() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}
