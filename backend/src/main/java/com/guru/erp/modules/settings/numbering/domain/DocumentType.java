package com.guru.erp.modules.settings.numbering.domain;

/**
 * ENT-006.document_type (FR-022). The kind of business document a numbering rule
 * issues sequence numbers for. Persisted by {@code name()} into a checked
 * {@code varchar(20)} column.
 */
public enum DocumentType {
    INVOICE,
    PO,
    PR,
    GRN,
    BILL,
    RECEIPT,
    DELIVERY,
    QUOTE,
    SO,
    JOURNAL
}
