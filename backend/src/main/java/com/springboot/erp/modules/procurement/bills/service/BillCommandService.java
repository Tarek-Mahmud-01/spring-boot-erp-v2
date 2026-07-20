package com.springboot.erp.modules.procurement.bills.service;

import com.springboot.erp.modules.procurement.bills.domain.BillStatus;
import com.springboot.erp.modules.procurement.bills.domain.MatchStatus;
import com.springboot.erp.modules.procurement.bills.domain.SupplierBill;
import com.springboot.erp.modules.procurement.bills.domain.SupplierBillGrnLink;
import com.springboot.erp.modules.procurement.bills.domain.SupplierBillLine;
import com.springboot.erp.modules.procurement.bills.domain.SupplierBillPoLink;
import com.springboot.erp.modules.procurement.bills.dto.BillDtos.BillCreateRequest;
import com.springboot.erp.modules.procurement.bills.dto.BillDtos.BillLineCreateRequest;
import com.springboot.erp.modules.procurement.bills.dto.BillDtos.BillLineUpdateRequest;
import com.springboot.erp.modules.procurement.bills.dto.BillDtos.BillResponse;
import com.springboot.erp.modules.procurement.bills.dto.BillDtos.BillUpdateRequest;
import com.springboot.erp.modules.procurement.bills.mapper.BillMapper;
import com.springboot.erp.modules.procurement.bills.repository.SupplierBillRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.id.Ulid;
import com.springboot.erp.platform.status.StateMachine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-030 SupplierBill (US-018 / FR-098–101): create (with 3-way match +
 * auto-approve), update (header any-status; lines Draft-only or totals-unchanged), manual approve,
 * and delete. Header+lines aggregate; the payable is posted as an outbox event on approval (see
 * {@link BillPostingService}) — finance is never called directly. One audit row per mutation.
 *
 * <p>Money is long minor units; qty is {@code BigDecimal}. Line net = round(qty × unitPrice)
 * (HALF_EVEN). PO-line discount + tax-code rate + PO invoice discount resolution live cross-slice
 * (reference resolved them from PO/tax-code tables); those are deferred to the finance/PO seam —
 * here tax is 0 and the bill total is the sum of line nets, so the AP figure is exact and stable.
 */
@Service
public class BillCommandService {

    static final String AUDIT_ENTITY = "supplier_bill";
    private static final String DEFAULT_CURRENCY = "USD";

    /** DRAFT → APPROVED_FOR_PAYMENT / INVOICED_NOT_RECEIVED (approve); posted terminal via payments. */
    private static final StateMachine<BillStatus> WORKFLOW = StateMachine.builder(BillStatus.class)
        .allow(BillStatus.DRAFT, BillStatus.RECEIVED, BillStatus.APPROVED_FOR_PAYMENT,
            BillStatus.INVOICED_NOT_RECEIVED, BillStatus.CANCELLED)
        .allow(BillStatus.RECEIVED, BillStatus.APPROVED_FOR_PAYMENT,
            BillStatus.INVOICED_NOT_RECEIVED, BillStatus.CANCELLED)
        .allow(BillStatus.INVOICED_NOT_RECEIVED, BillStatus.APPROVED_FOR_PAYMENT, BillStatus.CANCELLED)
        .allow(BillStatus.APPROVED_FOR_PAYMENT, BillStatus.PARTIALLY_PAID, BillStatus.PAID)
        .allow(BillStatus.PARTIALLY_PAID, BillStatus.PAID)
        .build();

    private final SupplierBillRepository repository;
    private final BillMapper mapper;
    private final AuditService audit;
    private final BillPostingService posting;

    public BillCommandService(SupplierBillRepository repository, BillMapper mapper,
                              AuditService audit, BillPostingService posting) {
        this.repository = repository;
        this.mapper = mapper;
        this.audit = audit;
        this.posting = posting;
    }

    @Transactional
    public BillResponse create(BillCreateRequest req) {
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "A bill must have at least one line");
        }
        String ccy = blank(req.currency()) ? DEFAULT_CURRENCY : req.currency().toUpperCase();
        SupplierBill bill = new SupplierBill();
        bill.setNumber(nextNumber());
        bill.setSupplierId(req.supplierId());
        bill.setPoId(req.poId());
        bill.setSupplierBillNo(req.supplierBillNo());
        bill.setBillDate(req.billDate());
        bill.setDueDate(req.dueDate());
        bill.setCurrency(ccy);
        bill.setNotes(req.notes());
        bill.setStatus(BillStatus.DRAFT);

        Set<MatchStatus> perLine = new HashSet<>();
        long subtotal = 0L;
        for (BillLineCreateRequest in : req.lines()) {
            SupplierBillLine line = new SupplierBillLine();
            line.setPoLineId(in.poLineId());
            line.setGrnLineId(in.grnLineId());
            line.setProductId(in.productId());
            line.setDescription(in.description());
            line.setQty(in.qty());
            line.setUnitPriceAmount(in.unitPriceAmount());
            line.setTaxCodeId(in.taxCodeId());
            long net = lineAmount(in.qty(), in.unitPriceAmount());
            line.setLineTotalAmount(net);
            MatchStatus ms = threeWayMatch(in.poLineId(), in.grnLineId());
            line.setMatchStatus(ms.wire());
            line.setCapitalItem(in.isCapitalItem());
            perLine.add(ms);
            subtotal += net;
            bill.addLine(line);
        }
        MatchStatus overall = perLine.size() == 1 ? perLine.iterator().next() : MatchStatus.MIXED;
        bill.setMatchStatus(overall.wire());
        bill.setSubtotalAmount(subtotal);
        bill.setTaxAmount(0L);
        bill.setTotalAmount(subtotal);

        if (req.grnIds() != null) {
            for (String grnId : new LinkedHashSet<>(req.grnIds())) {
                bill.addGrnLink(new SupplierBillGrnLink(grnId));
            }
        }
        for (String poId : distinctPoIds(req)) {
            bill.addPoLink(new SupplierBillPoLink(poId));
        }

        SupplierBill saved = repository.save(bill);
        audit.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, mapper.toResponse(saved));

        // FR-099: a clean 3-way match auto-approves and posts the payable.
        if (overall == MatchStatus.MATCHED) {
            saved.setStatus(WORKFLOW.transition(saved.getStatus(), BillStatus.APPROVED_FOR_PAYMENT));
            repository.save(saved);
            audit.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, null,
                mapper.toResponse(saved));
            posting.emitPosted(saved, false);
        }
        return mapper.toResponse(saved);
    }

    @Transactional
    public BillResponse update(String publicId, BillUpdateRequest req) {
        SupplierBill bill = load(publicId);
        checkVersion(bill, req.version());
        BillResponse before = mapper.toResponse(bill);

        // Header fields — editable in any status.
        bill.setSupplierBillNo(strip(req.supplierBillNo()));
        if (req.billDate() != null) {
            bill.setBillDate(req.billDate());
        }
        bill.setDueDate(req.dueDate());
        bill.setNotes(req.notes());
        if (!blank(req.currency())) {
            bill.setCurrency(req.currency().toUpperCase());
        }
        if (req.poId() != null) {
            bill.setPoId(req.poId().isBlank() ? null : req.poId());
        }

        if (req.lines() != null) {
            repository.deleteLinesByBillId(bill.getId());
            bill.clearLines();
            Set<MatchStatus> perLine = new HashSet<>();
            long subtotal = 0L;
            for (BillLineUpdateRequest in : req.lines()) {
                SupplierBillLine line = new SupplierBillLine();
                line.setPoLineId(in.poLineId());
                line.setGrnLineId(in.grnLineId());
                line.setProductId(in.productId());
                line.setDescription(in.description());
                line.setQty(in.qty());
                line.setUnitPriceAmount(in.unitPriceAmount());
                line.setTaxCodeId(in.taxCodeId());
                long net = lineAmount(in.qty(), in.unitPriceAmount());
                line.setLineTotalAmount(net);
                MatchStatus ms = threeWayMatch(in.poLineId(), in.grnLineId());
                line.setMatchStatus(ms.wire());
                line.setCapitalItem(in.isCapitalItem());
                perLine.add(ms);
                subtotal += net;
                bill.addLine(line);
            }
            // Past Draft the payable is posted; a changed total would desync AP → reject.
            if (bill.getStatus() != BillStatus.DRAFT && subtotal != bill.getSubtotalAmount()) {
                throw new DomainException(ErrorCode.CONFLICT,
                    "Bill " + bill.getNumber() + " is '" + bill.getStatus().wire()
                        + "'; its lines and amounts are locked because the payable is already posted.");
            }
            bill.setSubtotalAmount(subtotal);
            bill.setTaxAmount(0L);
            bill.setTotalAmount(subtotal);
            MatchStatus overall = perLine.size() == 1 ? perLine.iterator().next() : MatchStatus.MIXED;
            bill.setMatchStatus(overall.wire());
        }

        SupplierBill saved = repository.save(bill);
        audit.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** Manual approve — GRN-first posts the payable; bill-first parks in Invoiced Not Received. */
    @Transactional
    public BillResponse approve(String publicId, Long version) {
        SupplierBill bill = load(publicId);
        checkVersion(bill, version);
        if (bill.getStatus() != BillStatus.DRAFT && bill.getStatus() != BillStatus.RECEIVED) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Bill " + bill.getNumber() + " cannot be approved from status '" + bill.getStatus().wire() + "'");
        }
        BillResponse before = mapper.toResponse(bill);
        boolean billFirst = bill.getGrnLinks().isEmpty()
            && bill.getLines().stream().allMatch(l -> l.getGrnLineId() == null);
        BillStatus target = billFirst ? BillStatus.INVOICED_NOT_RECEIVED : BillStatus.APPROVED_FOR_PAYMENT;
        bill.setStatus(WORKFLOW.transition(bill.getStatus(), target));
        repository.save(bill);
        audit.record(AUDIT_ENTITY, bill.getPublicId(), AuditAction.UPDATE, before, mapper.toResponse(bill));
        posting.emitPosted(bill, billFirst);
        return mapper.toResponse(bill);
    }

    /** Delete: soft-delete; a posted bill first reverses its payable via the outbox. */
    @Transactional
    public void delete(String publicId) {
        SupplierBill bill = load(publicId);
        BillResponse before = mapper.toResponse(bill);
        boolean posted = bill.getStatus() == BillStatus.APPROVED_FOR_PAYMENT
            || bill.getStatus() == BillStatus.INVOICED_NOT_RECEIVED
            || bill.getStatus() == BillStatus.PARTIALLY_PAID
            || bill.getStatus() == BillStatus.PAID;
        if (posted) {
            posting.emitReversed(bill);
        }
        bill.softDelete();
        repository.save(bill);
        audit.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- money helpers --------------------------------------------------------

    /** qty × unitPrice rounded to minor units (HALF_EVEN) — qty is fractional (e.g. 12.675 kg). */
    static long lineAmount(BigDecimal qty, long unitPriceAmount) {
        return qty.multiply(BigDecimal.valueOf(unitPriceAmount))
            .setScale(0, RoundingMode.HALF_EVEN).longValueExact();
    }

    /**
     * FR-099 three-way match, collapsed to what this slice can see without the PO/GRN tables: a
     * line linked to a receipt is MATCHED; a PO-linked line with no receipt is OVER_INVOICED
     * (billing goods not yet usably received); a purely standalone line is MATCHED. The
     * price/qty-tolerance detail is the finance/PO consumer's job once it has the ordered figures.
     */
    private static MatchStatus threeWayMatch(String poLineId, String grnLineId) {
        if (poLineId == null) {
            return MatchStatus.MATCHED;
        }
        return grnLineId == null ? MatchStatus.OVER_INVOICED : MatchStatus.MATCHED;
    }

    private Set<String> distinctPoIds(BillCreateRequest req) {
        Set<String> ids = new LinkedHashSet<>();
        if (req.poId() != null && !req.poId().isBlank()) {
            ids.add(req.poId());
        }
        return ids;
    }

    private String nextNumber() {
        String number;
        do {
            number = "BILL-" + Ulid.next().substring(16);
        } while (repository.existsByNumber(number));
        return number;
    }

    private SupplierBill load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("SupplierBill", publicId));
    }

    private void checkVersion(SupplierBill bill, Long version) {
        if (version != null && version != bill.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private static String strip(String v) {
        return v == null || v.isBlank() ? null : v.strip();
    }
}
