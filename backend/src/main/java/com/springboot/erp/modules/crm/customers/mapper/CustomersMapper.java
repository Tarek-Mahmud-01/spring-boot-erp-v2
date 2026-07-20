package com.springboot.erp.modules.crm.customers.mapper;

import com.springboot.erp.modules.crm.customers.domain.ConsentLog;
import com.springboot.erp.modules.crm.customers.domain.Customer;
import com.springboot.erp.modules.crm.customers.domain.CustomerSegment;
import com.springboot.erp.modules.crm.customers.dto.CustomerDtos.ConsentLogEntry;
import com.springboot.erp.modules.crm.customers.dto.CustomerDtos.ConsentRecord;
import com.springboot.erp.modules.crm.customers.dto.CustomerDtos.CustomerResponse;
import com.springboot.erp.modules.crm.customers.dto.SegmentDtos.SegmentResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Entity->DTO mapping for the crm.customers slice (ARCHITECTURE.md §2).
 * {@code id} always maps from {@code publicId}; internal bigint ids are
 * never exposed. Hand-written (rather than MapStruct) because the response
 * shapes fold in the 1:1 {@code CustomerProfile} and derived member/consent
 * collections that don't map 1:1 from a single entity.
 */
@Component
public class CustomersMapper {

    public CustomerResponse toResponse(Customer c) {
        var p = c.getProfile();
        List<ConsentRecord> consents = new ArrayList<>();
        consents.add(new ConsentRecord("EMAIL",
            p != null && p.isConsentEmail(),
            p == null ? null : p.getConsentEmailAt(),
            p == null ? null : p.getConsentEmailBy()));
        consents.add(new ConsentRecord("SMS",
            p != null && p.isConsentSms(),
            p == null ? null : p.getConsentSmsAt(),
            p == null ? null : p.getConsentSmsBy()));

        return new CustomerResponse(
            c.getPublicId(),
            c.getCompanyId(),
            c.getMembershipId(),
            c.getFirstName(),
            c.getLastName(),
            c.getType().name(),
            c.getStatus().name(),
            p == null ? null : p.getMobile(),
            p == null ? null : p.getEmail(),
            p == null ? null : p.getPostcode(),
            p == null ? null : p.getDateOfBirth(),
            p == null ? null : p.getPreferredLocationId(),
            c.isAnalyticsConsent(),
            consents,
            c.getPhotoUrl(),
            c.isAnonymized(),
            c.getAnonymizedAt(),
            c.getVersion(),
            c.getCreatedAt(),
            c.getUpdatedAt());
    }

    public ConsentLogEntry toResponse(ConsentLog log) {
        return new ConsentLogEntry(
            log.getPublicId(),
            log.getChannel().name(),
            log.isGranted(),
            log.getRecordedAt(),
            log.getRecordedBy());
    }

    public SegmentResponse toResponse(CustomerSegment s, List<String> memberPublicIds) {
        List<Map<String, Object>> rules = s.getDefinition() == null ? List.of() : List.copyOf(s.getDefinition());
        return new SegmentResponse(
            s.getPublicId(),
            s.getCompanyId(),
            s.getCode(),
            s.getName(),
            s.getDescription(),
            s.getMode().name(),
            rules,
            s.getRuleLogic(),
            memberPublicIds,
            s.getRefreshedAt(),
            List.copyOf(s.getLinkedPromotionIds()),
            s.getVersion(),
            s.getCreatedAt(),
            s.getUpdatedAt());
    }
}
