package com.guru.erp.modules.finance.gl.domain;

/**
 * Optional subledger pointer on a {@link JournalLine} (reference {@code app.finance.constants.HolderType},
 * FR-243). {@code holderId} is a loose ULID pointing at a supplier/customer/employee record in another
 * module — no FK is enforced since the holder lives outside this slice.
 */
public enum HolderType {
    NONE,
    SUPPLIER,
    CUSTOMER,
    EMPLOYEE
}
