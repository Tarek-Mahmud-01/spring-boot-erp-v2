package com.springboot.erp.modules.settings.location.service;

import com.springboot.erp.modules.settings.company.domain.Company;
import com.springboot.erp.modules.settings.company.repository.CompanyRepository;
import com.springboot.erp.modules.settings.location.domain.Location;
import com.springboot.erp.modules.settings.location.domain.LocationStatus;
import com.springboot.erp.modules.settings.location.domain.LocationType;
import com.springboot.erp.modules.settings.location.domain.PriceDisplayMode;
import com.springboot.erp.modules.settings.location.dto.LocationDtos.LocationCreateRequest;
import com.springboot.erp.modules.settings.location.dto.LocationDtos.LocationResponse;
import com.springboot.erp.modules.settings.location.dto.LocationDtos.LocationUpdateRequest;
import com.springboot.erp.modules.settings.location.mapper.LocationMapper;
import com.springboot.erp.modules.settings.location.repository.LocationRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.web.PageResponse;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Location use-cases (ENT-002). Holds every business rule ported from the
 * reference {@code app.locations.views}: type-enum + IANA-timezone validation,
 * per-company code uniqueness, optimistic-lock on update, status transitions and
 * soft delete. Every mutation writes an audit row inside its own transaction.
 *
 * <p>Deviations from the reference (see returned notes): permissions collapse to
 * {@code settings.location.read/write}; delete is a soft delete rather than the
 * reference hard-delete; the outbox events and cross-module stock/history probes
 * are out of scope for this slice.
 */
@Service
public class LocationService {

    private static final String AUDIT_ENTITY = "location";

    private final LocationRepository repository;
    private final CompanyRepository companyRepository;
    private final LocationMapper mapper;
    private final AuditService auditService;

    public LocationService(LocationRepository repository, CompanyRepository companyRepository,
                           LocationMapper mapper, AuditService auditService) {
        this.repository = repository;
        this.companyRepository = companyRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<LocationResponse> list(String companyId, String status, String type,
                                               String q, Pageable pageable) {
        return PageResponse.of(
            repository.search(blankToNull(companyId), blankToNull(status), blankToNull(type),
                blankToNull(q), pageable),
            mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LocationResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    @Transactional
    public LocationResponse create(LocationCreateRequest request) {
        Company company = companyRepository.findByPublicId(request.companyId())
            .orElseThrow(() -> DomainException.notFound("Company", request.companyId()));
        String type = validateType(request.type());
        validateTimezone(request.timezone());
        String priceMode = validatePriceDisplayMode(request.priceDisplayMode());

        if (repository.existsByCompany_PublicIdAndCode(request.companyId(), request.code())) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "A location with code '%s' already exists for this company.".formatted(request.code()));
        }

        Location location = new Location();
        location.setCompany(company);
        location.setCode(request.code());
        location.setName(request.name().strip());
        location.setType(type);
        location.setTimezone(request.timezone());
        location.setAddress(mapper.toAddress(request.address()));
        location.setPhone(blankToNull(request.phone()));
        location.setPublicEmail(blankToNull(request.publicEmail()));
        location.setDefaultPriceListId(blankToNull(request.defaultPriceListId()));
        location.setDefaultTaxCodeId(blankToNull(request.defaultTaxCodeId()));
        location.setStatus(LocationStatus.ACTIVE.value());
        location.setPriceDisplayMode(priceMode);

        Location saved = repository.save(location);
        LocationResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, after);
        return after;
    }

    @Transactional
    public LocationResponse update(String publicId, LocationUpdateRequest request) {
        Location location = load(publicId);
        assertVersion(location, request.version());
        LocationResponse before = mapper.toResponse(location);

        if (request.timezone() != null) {
            validateTimezone(request.timezone());
            location.setTimezone(request.timezone());
        }
        if (request.type() != null) {
            location.setType(validateType(request.type()));
        }
        if (request.code() != null && !request.code().equals(location.getCode())) {
            if (repository.existsByCompany_PublicIdAndCodeAndIdNot(
                    location.getCompany().getPublicId(), request.code(), location.getId())) {
                throw new DomainException(ErrorCode.DUPLICATE, "Location code already in use for this company.");
            }
            location.setCode(request.code());
        }
        if (request.name() != null) {
            location.setName(request.name().strip());
        }
        if (request.address() != null) {
            location.setAddress(mapper.toAddress(request.address()));
        }
        if (request.phone() != null) {
            location.setPhone(blankToNull(request.phone()));
        }
        if (request.publicEmail() != null) {
            location.setPublicEmail(blankToNull(request.publicEmail()));
        }
        if (request.defaultPriceListId() != null) {
            location.setDefaultPriceListId(blankToNull(request.defaultPriceListId()));
        }
        if (request.defaultTaxCodeId() != null) {
            location.setDefaultTaxCodeId(blankToNull(request.defaultTaxCodeId()));
        }
        if (request.priceDisplayMode() != null) {
            location.setPriceDisplayMode(validatePriceDisplayMode(request.priceDisplayMode()));
        }

        LocationResponse after = mapper.toResponse(repository.save(location));
        auditService.record(AUDIT_ENTITY, location.getPublicId(), AuditAction.UPDATE, before, after);
        return after;
    }

    @Transactional
    public LocationResponse activate(String publicId) {
        return flipStatus(publicId, LocationStatus.ACTIVE);
    }

    @Transactional
    public LocationResponse deactivate(String publicId) {
        return flipStatus(publicId, LocationStatus.INACTIVE);
    }

    @Transactional
    public void delete(String publicId) {
        Location location = load(publicId);
        LocationResponse before = mapper.toResponse(location);
        location.softDelete();
        repository.save(location);
        auditService.record(AUDIT_ENTITY, location.getPublicId(), AuditAction.DELETE, before, null);
    }

    // --- internals ---

    private LocationResponse flipStatus(String publicId, LocationStatus target) {
        Location location = load(publicId);
        LocationResponse before = mapper.toResponse(location);
        location.setStatus(target.value());
        LocationResponse after = mapper.toResponse(repository.save(location));
        auditService.record(AUDIT_ENTITY, location.getPublicId(), AuditAction.UPDATE, before, after);
        return after;
    }

    private Location load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Location", publicId));
    }

    private void assertVersion(Location location, Long expected) {
        if (expected != null && expected != location.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                "Location was modified by someone else; reload and retry.");
        }
    }

    private String validateType(String value) {
        return LocationType.from(value)
            .map(LocationType::value)
            .orElseThrow(() -> new DomainException(ErrorCode.VALIDATION_FAILED,
                "The location type must be one of " + Arrays.stream(LocationType.values())
                    .map(LocationType::value).collect(Collectors.joining(", ")) + "."));
    }

    private void validateTimezone(String tz) {
        try {
            ZoneId.of(tz);
        } catch (DateTimeException ex) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Timezone must be a valid IANA zone.");
        }
    }

    private String validatePriceDisplayMode(String value) {
        if (blankToNull(value) == null) {
            return null;
        }
        return PriceDisplayMode.from(value)
            .map(PriceDisplayMode::value)
            .orElseThrow(() -> new DomainException(ErrorCode.VALIDATION_FAILED,
                "Price display mode must be INCLUSIVE or EXCLUSIVE."));
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
