package com.springboot.erp.modules.procurement.orders.service;

import com.springboot.erp.modules.procurement.orders.domain.PrStatus;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrder;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseRequisition;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseRequisitionLine;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoCreateRequest;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoLineRequest;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoResponse;
import com.springboot.erp.modules.procurement.orders.dto.PrDtos.PrTransitionRequest;
import com.springboot.erp.modules.procurement.orders.mapper.OrdersMapper;
import com.springboot.erp.modules.procurement.orders.repository.PurchaseRequisitionRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-084 — one-click PR → PO conversion. Creates a Draft PO from an UNDER_REVIEW requisition's
 * product-linked lines in a single transaction, then transitions the PR to CONVERTED so it can't be
 * converted twice. Kept in its own service so it can call both the PO command service and the PR
 * command service without a circular bean graph.
 *
 * <p>Ported reference rules (see {@code convert_to_po}):
 * <ul>
 *   <li>PR must be UNDER_REVIEW (the only state where CONVERTED is a valid transition).</li>
 *   <li>Free-text PR lines (no product) are skipped; at least one product line is required.</li>
 *   <li>The PR line's own price / tax / variant are honoured; downstream cost-defaulting from the
 *       product catalogue is deferred to the buyer editing the resulting Draft PO.</li>
 *   <li>Supplier defaults to the PR header supplier, else the first line's preferred supplier.
 *       Currency defaults to the PR header currency (else the PO's default). Company base-currency
 *       and exchange-rate resolution are deferred (cross-module).</li>
 *   <li>The order date is "now" — converting a requisition RAISES the order today, deliberately
 *       not back-dated to the requisition date.</li>
 * </ul>
 */
@Service
public class PurchaseRequisitionConversionService {

    private final PurchaseRequisitionRepository prRepository;
    private final PurchaseOrderCommandService poCommand;
    private final PurchaseRequisitionCommandService prCommand;
    private final OrdersMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public PurchaseRequisitionConversionService(PurchaseRequisitionRepository prRepository,
                                                PurchaseOrderCommandService poCommand,
                                                PurchaseRequisitionCommandService prCommand,
                                                OrdersMapper mapper) {
        this.prRepository = prRepository;
        this.poCommand = poCommand;
        this.prCommand = prCommand;
        this.mapper = mapper;
    }

    @Transactional
    public PoResponse convertToPo(String prPublicId) {
        PurchaseRequisition pr = prRepository.findByPublicId(prPublicId)
            .orElseThrow(() -> DomainException.notFound("PurchaseRequisition", prPublicId));

        if (pr.getStatus() != PrStatus.UNDER_REVIEW) {
            throw new DomainException(ErrorCode.ILLEGAL_STATE_TRANSITION,
                "Only an Under Review requisition can be converted to a PO (current status: "
                    + pr.getStatus().wire() + ").");
        }
        if (pr.getLocationId() == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Cannot convert a requisition with no location.");
        }

        String currency = pr.getCurrency() != null && !pr.getCurrency().isBlank()
            ? pr.getCurrency() : null;

        // Resolve supplier: PR header first, then the first line's preferred supplier.
        String supplierId = pr.getSupplierId();
        if (supplierId == null) {
            for (PurchaseRequisitionLine ln : pr.getLines()) {
                if (ln.getPreferredSupplierId() != null) {
                    supplierId = ln.getPreferredSupplierId();
                    break;
                }
            }
        }
        if (supplierId == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "No supplier on the requisition or its lines — pick one before converting.");
        }

        List<PoLineRequest> poLines = new ArrayList<>();
        for (PurchaseRequisitionLine ln : pr.getLines()) {
            if (ln.getProductId() == null) {
                continue; // free-text lines don't carry over
            }
            String lineCcy = ln.getUnitPrice() != null ? ln.getUnitPrice().currency() : currency;
            poLines.add(new PoLineRequest(
                ln.getProductId(),
                ln.getVariantId(),
                ln.getQty(),
                ln.getUomId(),
                ln.getUnitPrice() != null ? ln.getUnitPrice().amountMinor() : 0L,
                lineCcy,
                ln.getDiscountPercent() == null ? BigDecimal.ZERO : ln.getDiscountPercent(),
                ln.getTaxCodeId()));
        }
        if (poLines.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Cannot convert a requisition with no product lines.");
        }

        PoCreateRequest poReq = new PoCreateRequest(
            supplierId,
            pr.getLocationId(),
            Instant.now(clock),
            null,
            currency,
            BigDecimal.ONE,
            pr.getPaymentTerms(),
            pr.getPublicId(),
            null,
            pr.getInvoiceDiscountType(),
            pr.getInvoiceDiscountValue(),
            false,
            poLines);

        PurchaseOrder po = poCommand.createEntity(poReq);

        prCommand.transition(pr.getPublicId(), new PrTransitionRequest(
            PrStatus.CONVERTED.wire(), "Converted to " + po.getNumber(), null));

        return mapper.toResponse(po);
    }
}
