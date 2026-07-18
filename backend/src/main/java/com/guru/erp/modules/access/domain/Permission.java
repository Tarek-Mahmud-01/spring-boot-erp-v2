package com.guru.erp.modules.access.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A single permission code, e.g. {@code access.user.read}. Roles aggregate
 * permissions; the flattened set is embedded in the JWT for @PreAuthorize.
 */
@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 120)
    private String code;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "module", nullable = false, length = 64)
    private String module;

    @Column(name = "description", length = 500)
    private String description;

    protected Permission() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getModule() {
        return module;
    }

    public String getDescription() {
        return description;
    }
}
