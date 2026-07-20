package com.springboot.erp.modules.finance.coa.domain;

/** Which ERP module owns a particular account mapping (reference {@code AccountModule}). */
public enum AccountModule {
    INVENTORY,
    PROCUREMENT,
    SALES,
    FINANCE,
    POS
}
