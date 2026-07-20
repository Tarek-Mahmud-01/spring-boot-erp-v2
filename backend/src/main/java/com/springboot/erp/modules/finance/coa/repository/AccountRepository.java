package com.springboot.erp.modules.finance.coa.repository;

import com.springboot.erp.modules.finance.coa.domain.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for {@link Account} — the chart-of-accounts nested-set tree
 * (reference {@code views/chart_of_accounts.py}). {@code @SQLRestriction} on
 * {@code BaseEntity} excludes soft-deleted rows automatically; this slice's
 * delete path is a hard delete (see {@link Account} javadoc), but the
 * restriction is kept for platform consistency.
 *
 * <p>The {@code shift*} methods are bulk {@code UPDATE ... WHERE ...} statements
 * used ONLY by {@code NestedSetService} to open/close/relocate a lft/rgt range —
 * they intentionally bypass the entity's optimistic-lock/audit path because a
 * single insert/delete/move can touch many sibling rows and the reference
 * itself uses raw SQL UPDATEs here (not a per-row Python loop), for the same
 * reason. Each shift explicitly repeats the {@code deleted_at is null}
 * predicate so a soft-deleted row can never participate (mirrors
 * {@code nested_set.py}'s own {@code Account.deleted_at.is_(None)} filters).
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByPublicId(String publicId);

    Optional<Account> findByCompanyIdAndCodeAndDeletedAtIsNull(String companyId, String code);

    boolean existsByParentIdAndDeletedAtIsNull(Long parentId);

    Page<Account> findByCompanyId(String companyId, Pageable pageable);

    /** Unpaged — used by the CSV import's forward-reference resolution and nested-set rebuild. */
    List<Account> findAllByCompanyId(String companyId);

    @Query(value = """
        select a from Account a
        where a.companyId = :companyId
          and (lower(a.code) like :q or lower(a.name) like :q)
        order by a.lft asc, a.code asc
        """,
        countQuery = """
        select count(a) from Account a
        where a.companyId = :companyId
          and (lower(a.code) like :q or lower(a.name) like :q)
        """)
    Page<Account> search(@Param("companyId") String companyId, @Param("q") String q, Pageable pageable);

    @Query("select coalesce(max(a.rgt), 0) from Account a where a.companyId = :companyId and a.deletedAt is null")
    int maxRgt(@Param("companyId") String companyId);

    // ------------------------------------------------------------------
    // Nested-set incremental shifts (Joe Celko modified preorder traversal).
    // Faithful port of app.finance.nested_set — bulk UPDATEs touching only
    // the slice of the tree at/right of the mutation point, never a full
    // tree rebuild.
    // ------------------------------------------------------------------

    // Every shift below uses clearAutomatically + flushAutomatically: bulk
    // JPQL UPDATEs bypass the persistence context, so any Account instance the
    // caller (NestedSetService) is still holding would otherwise read back
    // stale lft/rgt/depth values after this runs. Flushing first pushes any
    // pending managed-entity changes to SQL in the correct order; clearing
    // after forces every subsequent read (including re-fetching the moved
    // node/parent) to go back to the database.

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Account a set a.rgt = a.rgt + :width " +
           "where a.companyId = :companyId and a.deletedAt is null and a.rgt >= :threshold")
    void shiftRgtGte(@Param("companyId") String companyId, @Param("threshold") int threshold, @Param("width") int width);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Account a set a.lft = a.lft + :width " +
           "where a.companyId = :companyId and a.deletedAt is null and a.lft > :threshold")
    void shiftLftGt(@Param("companyId") String companyId, @Param("threshold") int threshold, @Param("width") int width);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Account a set a.lft = a.lft - :width " +
           "where a.companyId = :companyId and a.deletedAt is null and a.lft > :threshold")
    void shrinkLftGt(@Param("companyId") String companyId, @Param("threshold") int threshold, @Param("width") int width);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Account a set a.rgt = a.rgt - :width " +
           "where a.companyId = :companyId and a.deletedAt is null and a.rgt > :threshold")
    void shrinkRgtGt(@Param("companyId") String companyId, @Param("threshold") int threshold, @Param("width") int width);

    /** STEP 2 of move — negate lft/rgt for the whole subtree (temporary removal). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Account a set a.lft = -a.lft, a.rgt = -a.rgt " +
           "where a.companyId = :companyId and a.deletedAt is null and a.lft between :oldLft and :oldRgt")
    void negateSubtree(@Param("companyId") String companyId, @Param("oldLft") int oldLft, @Param("oldRgt") int oldRgt);

    /** STEP 5 of move — re-insert the negated subtree at its new position, depth-shifted. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Account a set a.lft = (-a.lft) + :offset, a.rgt = (-a.rgt) + :offset, a.depth = a.depth + :depthDelta " +
           "where a.companyId = :companyId and a.lft < 0")
    void reinsertNegatedSubtree(@Param("companyId") String companyId, @Param("offset") int offset, @Param("depthDelta") int depthDelta);

    @Query("select coalesce(max(a.rgt), 0) from Account a where a.companyId = :companyId and a.deletedAt is null and a.lft > 0")
    int maxRgtPositive(@Param("companyId") String companyId);
}
