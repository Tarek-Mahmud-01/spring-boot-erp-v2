package com.guru.erp.modules.finance.coa.service;

import com.guru.erp.modules.finance.coa.domain.Account;
import com.guru.erp.modules.finance.coa.repository.AccountRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nested-set (modified preorder traversal) operations for the chart of
 * accounts — faithful port of the reference {@code app.finance.nested_set}.
 *
 * <p>The canonical "Joe Celko" representation: each row carries {@code lft} and
 * {@code rgt} integers laid out so that for any node A and any descendant D:
 *
 * <pre>    A.lft &lt; D.lft &lt; D.rgt &lt; A.rgt</pre>
 *
 * That single inequality gives O(1) tests for ancestry, single-query subtree
 * fetches, and O(1) descendant counts ({@code (rgt - lft - 1) / 2}). Inserts
 * and deletes are INCREMENTAL — they shift only the affected slice of the tree
 * to the right of the mutation point via SQL {@code UPDATE}, never a full
 * tree rebuild. Reference layout the algorithms keep invariant:
 *
 * <pre>
 *   Assets               (lft=1,  rgt=12)
 *     Current Assets     (lft=2,  rgt=7)
 *       Cash             (lft=3,  rgt=4)
 *       Accounts Recv.   (lft=5,  rgt=6)
 *     Fixed Assets       (lft=8,  rgt=11)
 *       Equipment        (lft=9,  rgt=10)
 * </pre>
 *
 * Rules: parent always has smaller {@code lft} and larger {@code rgt} than every
 * child; subtree width is always even ({@code rgt - lft = 2 * descendantCount + 1});
 * inserts shift right-side values, deletes close the gap; renames/status flips
 * never touch {@code lft}/{@code rgt}.
 *
 * <p>Every method here runs {@code MANDATORY} — it must be called from within
 * an existing {@code @Transactional} boundary (the owning service's create /
 * update / delete), exactly like {@code AuditService.record} and
 * {@code OutboxPublisher.publish}, so the shift and the row mutation it
 * supports commit atomically or not at all.
 */
@Service
public class NestedSetService {

    /** A reserved (lft, rgt, depth) for a node about to be inserted. */
    public record NestedSetSlot(int lft, int rgt, int depth) {
    }

    private final AccountRepository repository;

    public NestedSetService(AccountRepository repository) {
        this.repository = repository;
    }

    // ------------------------------------------------------------------
    // INSERT — make room then return the new slot
    // ------------------------------------------------------------------

    /**
     * Open a 2-wide gap in the nested set and return the new node's slot.
     * Caller is responsible for persisting the new {@link Account} row with the
     * returned lft/rgt/depth immediately after this call, inside the same
     * transaction. The shift updates only touch rows whose right edge is at or
     * past the insertion point — typical case is O(small).
     *
     * <p>Algorithm (classic Celko):
     * <ul>
     *   <li>Root insert: {@code lft = MAX(rgt) + 1}, {@code rgt = lft + 1}, {@code depth = 0}.</li>
     *   <li>Child insert (parent P):
     *     <ol>
     *       <li>{@code UPDATE accounts SET rgt = rgt + 2 WHERE rgt >= P.rgt} — opens space
     *           to the right of P (and bumps P itself).</li>
     *       <li>{@code UPDATE accounts SET lft = lft + 2 WHERE lft > P.rgt} — shifts every
     *           node strictly to the right of P.</li>
     *       <li>New slot: {@code lft = P.rgt}, {@code rgt = P.rgt + 1}.</li>
     *     </ol>
     *   </li>
     * </ul>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public NestedSetSlot reserveInsertSlot(String companyId, Account parent) {
        if (parent == null) {
            int maxRgt = repository.maxRgt(companyId);
            return new NestedSetSlot(maxRgt + 1, maxRgt + 2, 0);
        }

        // Record parent.rgt / parent.depth BEFORE we shift it.
        int parentRgt = parent.getRgt();
        int parentDepth = parent.getDepth();

        // Step 1: open the 2-wide gap to the right of the parent's right edge.
        // The ">=" predicate is essential — parent.rgt itself bumps up too so
        // the new child fits inside the parent's range.
        repository.shiftRgtGte(companyId, parentRgt, 2);
        repository.shiftLftGt(companyId, parentRgt, 2);

        return new NestedSetSlot(parentRgt, parentRgt + 1, parentDepth + 1);
    }

    // ------------------------------------------------------------------
    // DELETE — close the gap left behind
    // ------------------------------------------------------------------

    /**
     * Shrink the right side of the tree by the deleted subtree's width. Call
     * this AFTER the row(s) have been deleted. {@code oldLft}/{@code oldRgt} are
     * the bounds the deleted node owned at delete time. Works for leaves
     * (width=2) and full subtrees (width = oldRgt - oldLft + 1).
     *
     * <p>Both predicates target strictly to the right of the deleted range, so
     * siblings on the left and ancestors that contained the deleted subtree are
     * correctly contracted (ancestor rgt > oldRgt so its rgt drops; its
     * lft &lt; oldLft &lt; oldRgt so its lft is left alone — ancestor narrows).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void closeDeleteGap(String companyId, int oldLft, int oldRgt) {
        int width = oldRgt - oldLft + 1;
        if (width <= 0) {
            return;
        }
        repository.shrinkLftGt(companyId, oldRgt, width);
        repository.shrinkRgtGt(companyId, oldRgt, width);
    }

    // ------------------------------------------------------------------
    // MOVE — relocate a subtree under a new parent
    // ------------------------------------------------------------------

    /**
     * Re-parent {@code node} (and every descendant) under {@code newParent}.
     * Implements the canonical 5-step "move subtree" algorithm:
     *
     * <pre>
     * STEP 1  Get node range  -&gt;  oldLft = L, oldRgt = R, width = R-L+1
     * STEP 2  Mark subtree negative (temporary remove)
     *         UPDATE SET lft=-lft, rgt=-rgt WHERE lft BETWEEN L AND R
     * STEP 3  Close gap at old position
     *         UPDATE SET lft=lft-width WHERE lft &gt; R
     *         UPDATE SET rgt=rgt-width WHERE rgt &gt; R
     * STEP 4  Open space at new parent (capture P = newParent.rgt AFTER step 3's shift)
     *         UPDATE SET rgt=rgt+width WHERE rgt &gt;= P
     *         UPDATE SET lft=lft+width WHERE lft &gt; P
     * STEP 5  Re-insert negated subtree at new slot
     *         UPDATE SET lft=-lft+(P-L), rgt=-rgt+(P-L) WHERE lft &lt; 0
     * </pre>
     *
     * <p>Important subtleties:
     * <ul>
     *   <li>The new parent's {@code rgt} is captured AFTER step 3 runs (a fresh
     *       repository read), because step 3 itself may have shifted it if the
     *       new parent sits to the right of the moving subtree. The offset
     *       {@code P - L} uses that post-step-3 value — this port re-reads the
     *       parent row rather than trusting the caller's in-memory instance,
     *       exactly like the reference's {@code db.refresh(new_parent)}.</li>
     *   <li>Cycle check first: refuses to move a node under itself or any of its
     *       descendants ({@code newParent.lft >= node.lft && newParent.rgt <= node.rgt}
     *       would mark the "new parent" as a descendant of the node being moved).</li>
     * </ul>
     *
     * @throws DomainException VALIDATION_FAILED if {@code newParent} is {@code node}
     *         itself or one of its own descendants (a cycle).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void moveSubtree(String companyId, Account node, Account newParent) {
        // ------------------------------------------------------------
        // Cycle check — newParent must NOT be within node's own subtree.
        // ------------------------------------------------------------
        if (newParent != null && newParent.getLft() >= node.getLft() && newParent.getRgt() <= node.getRgt()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Cannot move an account under itself or one of its own descendants.");
        }

        // ------------------------------------------------------------
        // STEP 1 — Get node details.
        // ------------------------------------------------------------
        int oldLft = node.getLft();
        int oldRgt = node.getRgt();
        int width = oldRgt - oldLft + 1;
        int oldDepth = node.getDepth();

        // ------------------------------------------------------------
        // STEP 2 — Mark subtree negative (temporary removal).
        // "lft BETWEEN oldLft AND oldRgt" selects exactly the subtree: every
        // descendant satisfies oldLft < lft < oldRgt; the root itself
        // satisfies lft = oldLft. Negate BOTH lft and rgt so the subtree's
        // relative ordering is preserved in negative space.
        // ------------------------------------------------------------
        repository.negateSubtree(companyId, oldLft, oldRgt);

        // ------------------------------------------------------------
        // STEP 3 — Close the gap at the old position.
        // Only POSITIVE rows are affected (negative rows are out of band).
        // Ancestor rgt drops; siblings to the right of the old position
        // slide left by width.
        // ------------------------------------------------------------
        repository.shrinkLftGt(companyId, oldRgt, width);
        repository.shrinkRgtGt(companyId, oldRgt, width);

        // ------------------------------------------------------------
        // STEP 4 — Create space at the new parent.
        // Capture newParent.rgt FIRST (after step 3's shift may have moved
        // it) — that value, P, is what step 5 uses for the offset. For a
        // root move, P = (max_rgt over positive rows) + 1.
        // ------------------------------------------------------------
        int newParentRgt;
        int newDepthForRoot;
        if (newParent == null) {
            newParentRgt = repository.maxRgtPositive(companyId) + 1;
            newDepthForRoot = 0;
        } else {
            Account refreshed = repository.findById(newParent.getId())
                .orElseThrow(() -> DomainException.notFound("Account", newParent.getPublicId()));
            newParentRgt = refreshed.getRgt();
            newDepthForRoot = refreshed.getDepth() + 1;
            repository.shiftRgtGte(companyId, newParentRgt, width);
            repository.shiftLftGt(companyId, newParentRgt, width);
        }

        // ------------------------------------------------------------
        // STEP 5 — Re-insert the negated subtree at the new position.
        // newLft = -lft + (newParentRgt - oldLft); newRgt likewise.
        // depth shifts by (newDepthForRoot - oldDepth) so children land at
        // the right level under the new parent.
        // ------------------------------------------------------------
        int offset = newParentRgt - oldLft;
        int depthDelta = newDepthForRoot - oldDepth;
        repository.reinsertNegatedSubtree(companyId, offset, depthDelta);
    }

    // ------------------------------------------------------------------
    // READ HELPERS — pure containment checks, no DB round-trip needed.
    // ------------------------------------------------------------------

    /** Whether {@code node} (by lft/rgt) is a strict descendant of {@code ancestor}. */
    public boolean isDescendant(int nodeLft, int nodeRgt, Account ancestor) {
        return ancestor.getLft() < nodeLft && nodeRgt < ancestor.getRgt();
    }
}
