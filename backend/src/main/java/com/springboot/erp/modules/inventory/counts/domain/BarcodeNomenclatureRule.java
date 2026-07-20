package com.springboot.erp.modules.inventory.counts.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-046 BarcodeNomenclatureRule — parsing rules for scale-printed
 * variable-weight barcodes (GS1 prefix 02 / 20-29). The POS reads these at
 * startup to split a scanned barcode into an item reference and an embedded
 * weight/price measure.
 *
 * <p>{@code companyId} is a loose ULID cross-slice reference.
 * {@code measureScale} is NUMERIC(10,6) (a scale factor, not money — BigDecimal,
 * never double). {@code ruleType} persists via {@link BarcodeRuleTypeConverter}
 * as the lowercase wire value to satisfy
 * {@code ck_barcode_nomenclature_rule_type}.
 */
@Entity
@Table(name = "barcode_nomenclature_rules")
public class BarcodeNomenclatureRule extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** GS1 prefix that triggers this rule (e.g. "02" or "20"). */
    @Column(name = "prefix", nullable = false, length = 4)
    private String prefix;

    @Convert(converter = BarcodeRuleTypeConverter.class)
    @Column(name = "rule_type", nullable = false, length = 20)
    private BarcodeRuleType ruleType;

    /** Digits after the prefix that encode the item reference. */
    @Column(name = "item_digits", nullable = false)
    private int itemDigits = 5;

    /** Digits that encode the weight/price measure. */
    @Column(name = "measure_digits", nullable = false)
    private int measureDigits = 5;

    /** Divide the raw measure integer by this to get the real value. */
    @Column(name = "measure_scale", nullable = false, precision = 10, scale = 6)
    private BigDecimal measureScale = new BigDecimal("0.001");

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public BarcodeNomenclatureRule() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public BarcodeRuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(BarcodeRuleType ruleType) {
        this.ruleType = ruleType;
    }

    public int getItemDigits() {
        return itemDigits;
    }

    public void setItemDigits(int itemDigits) {
        this.itemDigits = itemDigits;
    }

    public int getMeasureDigits() {
        return measureDigits;
    }

    public void setMeasureDigits(int measureDigits) {
        this.measureDigits = measureDigits;
    }

    public BigDecimal getMeasureScale() {
        return measureScale;
    }

    public void setMeasureScale(BigDecimal measureScale) {
        this.measureScale = measureScale;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}
