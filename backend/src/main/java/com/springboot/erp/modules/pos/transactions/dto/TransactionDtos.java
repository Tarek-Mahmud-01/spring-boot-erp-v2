package com.springboot.erp.modules.pos.transactions.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for the POS "transactions" sub-slice (records — ARCHITECTURE.md §2). Mirrors the
 * reference Pydantic shapes in {@code app.pos.schemas}: {@code TransactionOpenRequest},
 * {@code AddLineRequest}, {@code UpdateLineRequest}, {@code TenderRequest}, {@code AgeVerifyRequest},
 * {@code PosLineResponse}, {@code PosTenderResponse}, {@code PosTransactionResponse}. Cross-slice
 * ids are ULID {@code char(26)} strings; money is a long minor-unit amount paired with the header's
 * {@code currency} (never a float); {@code qty} is {@link BigDecimal}(12,3) to support weighed goods.
 */
public final class TransactionDtos {

    private TransactionDtos() {
    }

    /**
     * POST /api/pos/transactions body — open a new cart on a register. {@code locationId} /
     * {@code currency} are the register's owning Location and the company's base currency
     * (reference {@code open_transaction} resolves both server-side from the Register row); this
     * slice never hard-calls the registers/settings modules, so the caller — a thin façade that
     * already looked the register up — supplies them directly.
     */
    public record TransactionOpenRequest(
        @NotNull @Size(min = 26, max = 26) String registerId,
        @Size(min = 26, max = 26) String customerId,
        @NotNull @Size(min = 26, max = 26) String locationId,
        @NotNull @Size(min = 3, max = 3) String currency
    ) {
    }

    /** PATCH /api/pos/transactions/{id}/customer body — attach or (with null) detach a customer. */
    public record AttachCustomerRequest(
        @Size(min = 26, max = 26) String customerId
    ) {
    }

    /**
     * POST /api/pos/transactions/{id}/lines body — add a product line to the cart. Reference
     * {@code add_line_by_product} resolves price/tax/restriction/SKU from the product catalogue
     * (Product-module concern, {@code product_views.resolve_sale_context}); this slice never
     * hard-calls that module, so the caller — a thin façade that already resolved the sale context
     * — supplies the already-priced snapshot fields directly.
     */
    public record AddLineRequest(
        @NotNull @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal qty,
        @NotNull @Size(max = 50) String sku,
        @NotNull @Size(max = 200) String name,
        @Size(max = 64) String barcode,
        @NotNull @Min(0) long unitPriceAmount,
        @Min(0) Long basePriceAmount,
        @Size(min = 26, max = 26) String taxCodeId,
        @NotNull @DecimalMin("0") BigDecimal taxRatePercent,
        boolean taxInclusive,
        boolean soldByWeight,
        boolean isRestricted18,
        boolean isRestricted21,
        boolean isRestrictedControlledDisplay
    ) {
    }

    /** PATCH /api/pos/transactions/{id}/lines/{lineId} body — change a line's quantity. */
    public record UpdateLineRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal qty
    ) {
    }

    /**
     * POST /api/pos/transactions/{id}/tenders body — add a payment leg. Reference {@code add_tender}
     * resolves the payment method's type + cash-ness from the {@code PaymentMethod} row
     * (payment-methods module concern); this slice never hard-calls that module, so the caller —
     * already holding the resolved method — supplies {@code methodType} and {@code isCash} directly.
     */
    public record TenderRequest(
        @NotNull @Size(min = 26, max = 26) String paymentMethodId,
        @NotNull @Size(max = 20) String methodType,
        boolean isCash,
        @Min(1) long amount,
        @Min(0) Long tenderedAmount,
        @Size(max = 120) String reference,
        @Size(max = 8) String maskedPan
    ) {
    }

    /** POST /api/pos/transactions/{id}/age-verify body. */
    public record AgeVerifyRequest(
        @Size(max = 40) String idType
    ) {
    }

    /** POST /api/pos/transactions/{id}/void body. */
    public record VoidTransactionRequest(
        @Size(max = 500) String reason
    ) {
    }

    /** Line read shape (reference {@code PosLineResponse}). */
    public record PosLineResponse(
        String id,
        int lineNo,
        String productId,
        String variantId,
        String sku,
        String name,
        String barcode,
        BigDecimal qty,
        BigDecimal freeQty,
        long unitPriceAmount,
        long basePriceAmount,
        String currency,
        long discountAmount,
        String promotionLabel,
        String taxCodeId,
        BigDecimal taxRate,
        boolean taxInclusive,
        long taxAmount,
        long lineNetAmount,
        long lineTotalAmount,
        boolean isRestricted18,
        boolean isRestricted21,
        boolean isRestrictedControlledDisplay,
        String rewardPromotionId,
        BigDecimal weighedQtyKg
    ) {
    }

    /** Tender read shape (reference {@code PosTenderResponse}). */
    public record PosTenderResponse(
        String id,
        int sequence,
        String paymentMethodId,
        String methodType,
        long amountAmount,
        String amountCurrency,
        Long tenderedAmount,
        long changeAmount,
        String reference,
        String maskedPan,
        boolean isReversed
    ) {
    }

    /**
     * Transaction (header + lines + tenders) read shape (reference {@code PosTransactionResponse}).
     * {@code id} is the ULID public id. {@code balanceAmount} = {@code totalAmount} − sum of active
     * (non-reversed) tenders, derived at read time (never persisted).
     */
    public record PosTransactionResponse(
        String id,
        String registerId,
        String locationId,
        String customerId,
        String type,
        String status,
        String receiptNumber,
        String documentType,
        long subtotalAmount,
        long taxAmount,
        long discountAmount,
        long manualDiscountAmount,
        String managerApprovalName,
        long surchargeAmount,
        long surchargeTaxAmount,
        boolean surchargeTaxable,
        String surchargeLabel,
        long totalAmount,
        long paidAmount,
        long changeAmount,
        long balanceAmount,
        String currency,
        boolean ageVerified,
        boolean ageRequired,
        String ageIdType,
        int reprintCount,
        String refundOfId,
        Instant occurredAt,
        Instant completedAt,
        long version,
        List<PosLineResponse> lines,
        List<PosTenderResponse> tenders
    ) {
    }
}
