package com.springboot.erp.modules.finance.coa.domain;

/**
 * ENT-016.posting_type — FR-224 (reference {@code app.finance.constants.AccountPostingType}).
 * {@code HEADER} accounts are structural (group) nodes and can never receive a
 * posting; only {@code POSTING} accounts are postable leaves.
 */
public enum AccountPostingType {
    POSTING,
    HEADER
}
