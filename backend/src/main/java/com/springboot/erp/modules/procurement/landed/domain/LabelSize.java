package com.springboot.erp.modules.procurement.landed.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A barcode-label stock size offered in the Print Label dialog (reference
 * {@code app.procurement.models.LabelSize}). {@code widthMm} is the dimension across the print head
 * (roll width); {@code heightMm} is the feed length. Seeded in V45 and served read-only. Reproduces
 * the unique {@code name}.
 */
@Entity
@Table(name = "label_sizes")
public class LabelSize extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "width_mm", nullable = false)
    private int widthMm;

    @Column(name = "height_mm", nullable = false)
    private int heightMm;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public LabelSize() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(int widthMm) {
        this.widthMm = widthMm;
    }

    public int getHeightMm() {
        return heightMm;
    }

    public void setHeightMm(int heightMm) {
        this.heightMm = heightMm;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
