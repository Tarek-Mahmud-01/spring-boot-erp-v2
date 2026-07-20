package com.springboot.erp.modules.crm.customers.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-073 CustomerSegment — static or dynamic customer grouping for
 * marketing/pricing (reference {@code CustomerSegment}, US-042 /
 * FR-219-222). {@code companyId} is a loose cross-module ULID reference
 * (settings.company) — no hard FK.
 *
 * <p>{@code definition} is the dynamic rule list (each element:
 * {@code field}/{@code op}/{@code value}/{@code window_days}/{@code currency});
 * {@code linkedPromotionIds} are loose cross-module ULID references
 * (products.promotion). Constraints reproduced in V60: unique
 * {@code (company_id, code)}, {@code mode} check.
 */
@Entity
@Table(name = "customer_segments", uniqueConstraints = @UniqueConstraint(name = "uq_customer_segments_company_code", columnNames = {"company_id", "code"}))
public class CustomerSegment extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 8)
    private SegmentMode mode = SegmentMode.STATIC;

    /** FR-219 — dynamic rule list: [{field, op, value, window_days, currency}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "definition", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> definition = new ArrayList<>();

    /** FR-219 — how multiple rules combine: AND (all must match) or OR (any match). */
    @Column(name = "rule_logic", nullable = false, length = 3)
    private String ruleLogic = "AND";

    /** FR-221 — linked products.promotion public ids (loose cross-module refs). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "linked_promotion_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> linkedPromotionIds = new ArrayList<>();

    @Column(name = "refreshed_at")
    private Instant refreshedAt;

    @OneToMany(mappedBy = "segment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<CustomerSegmentMember> members = new ArrayList<>();

    public CustomerSegment() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SegmentMode getMode() {
        return mode;
    }

    public void setMode(SegmentMode mode) {
        this.mode = mode;
    }

    public List<Map<String, Object>> getDefinition() {
        return definition;
    }

    public void setDefinition(List<Map<String, Object>> definition) {
        this.definition = definition == null ? new ArrayList<>() : definition;
    }

    public String getRuleLogic() {
        return ruleLogic;
    }

    public void setRuleLogic(String ruleLogic) {
        this.ruleLogic = ruleLogic;
    }

    public List<String> getLinkedPromotionIds() {
        return linkedPromotionIds;
    }

    public void setLinkedPromotionIds(List<String> linkedPromotionIds) {
        this.linkedPromotionIds = linkedPromotionIds == null ? new ArrayList<>() : linkedPromotionIds;
    }

    public Instant getRefreshedAt() {
        return refreshedAt;
    }

    public void setRefreshedAt(Instant refreshedAt) {
        this.refreshedAt = refreshedAt;
    }

    public List<CustomerSegmentMember> getMembers() {
        return members;
    }

    public void addMember(CustomerSegmentMember member) {
        member.setSegment(this);
        members.add(member);
    }

    public void clearMembers() {
        members.clear();
    }
}
