package com.springboot.erp.modules.procurement.suppliers.domain;

/**
 * ENT-026 Supplier trade type (reference {@code app.procurement.constants.SupplierType}).
 *
 * <p>Persisted UPPERCASE ({@code GOODS} / {@code SERVICES} / {@code BOTH}) via
 * {@code @Enumerated(STRING)} — the constant name equals the wire value, matching the
 * {@code ck_suppliers_type} check constraint, so no {@code AttributeConverter} is needed.
 */
public enum SupplierType {
    GOODS,
    SERVICES,
    BOTH
}
