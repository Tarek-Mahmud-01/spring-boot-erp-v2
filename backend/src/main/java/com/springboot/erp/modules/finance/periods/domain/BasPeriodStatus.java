package com.springboot.erp.modules.finance.periods.domain;

/**
 * ENT-AU-001 BasPeriod lifecycle (reference {@code app.finance.models_bas.BasPeriodStatus}, FR-AU-023).
 * OPEN -&gt; LODGED -&gt; FROZEN, with an explicit unfreeze (FROZEN -&gt; OPEN) and un-lodge (LODGED -&gt; OPEN)
 * override — both require the elevated {@code finance.period.adjust} permission, enforced in the
 * service (the state machine only knows which moves are topologically legal, not who may make them).
 */
public enum BasPeriodStatus {
    OPEN,
    LODGED,
    FROZEN
}
