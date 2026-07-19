package com.guru.erp.modules.finance.gl.domain;

/**
 * ENT-046 JournalEntry lifecycle (reference {@code app.finance.constants.JournalEntryStatus}).
 * DRAFT -&gt; POSTED -&gt; REVERSED. Both POSTED and REVERSED are immutable — nothing ever leaves
 * REVERSED (a reversal entry itself is born already POSTED and stays there).
 */
public enum JournalEntryStatus {
    DRAFT,
    POSTED,
    REVERSED
}
