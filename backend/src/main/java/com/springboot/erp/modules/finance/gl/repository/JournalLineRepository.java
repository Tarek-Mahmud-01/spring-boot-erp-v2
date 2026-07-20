package com.springboot.erp.modules.finance.gl.repository;

import com.springboot.erp.modules.finance.gl.domain.JournalLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link JournalLine}. Lines are normally reached via {@code JournalEntry.getLines()}. */
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    List<JournalLine> findByEntryIdOrderByLineNo(Long entryId);

    /** Audit M1 (reference) — used by a future CoA slice to block deleting a posted-to account. */
    boolean existsByAccountId(String accountId);
}
