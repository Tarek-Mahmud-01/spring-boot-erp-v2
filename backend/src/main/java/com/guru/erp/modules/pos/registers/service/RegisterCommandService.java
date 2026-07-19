package com.guru.erp.modules.pos.registers.service;

import com.guru.erp.modules.pos.registers.domain.PeripheralConnection;
import com.guru.erp.modules.pos.registers.domain.PeripheralType;
import com.guru.erp.modules.pos.registers.domain.Register;
import com.guru.erp.modules.pos.registers.domain.RegisterOperatingMode;
import com.guru.erp.modules.pos.registers.domain.RegisterPeripheral;
import com.guru.erp.modules.pos.registers.domain.RegisterStatus;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.PeripheralBindRequest;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.PeripheralPayload;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.PeripheralResponse;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.PeripheralUpdateRequest;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.RegisterCreateRequest;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.RegisterResponse;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.RegisterUpdateRequest;
import com.guru.erp.modules.pos.registers.mapper.RegistersMapper;
import com.guru.erp.modules.pos.registers.repository.RegisterPeripheralRepository;
import com.guru.erp.modules.pos.registers.repository.RegisterRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.outbox.OutboxPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-060 Register + ENT-060a RegisterPeripheral
 * (US-005.1 / FR-25.1..25.9). Ports the reference create / update / delete /
 * bind-peripheral / update-peripheral flows: unique {@code (location, code)},
 * a hard-delete guard on register history (reference
 * {@code RegisterHasHistoryError} — mirrored here via
 * {@link RegisterHistoryGuard}, a probe seam the till-session slice satisfies
 * in-module), and one audit row + outbox event per mutation.
 */
@Service
public class RegisterCommandService {

    static final String AUDIT_REGISTER = "register";
    static final String AUDIT_PERIPHERAL = "register_peripheral";
    static final String EVENT_REGISTER_CREATED = "pos.register.created";
    static final String EVENT_REGISTER_UPDATED = "pos.register.updated";
    static final String EVENT_REGISTER_DEACTIVATED = "pos.register.deactivated";
    static final String EVENT_PERIPHERAL_BOUND = "pos.register_peripheral.bound";
    static final String EVENT_PERIPHERAL_UPDATED = "pos.register_peripheral.updated";

    private final RegisterRepository registerRepository;
    private final RegisterPeripheralRepository peripheralRepository;
    private final RegistersMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outbox;
    private final RegisterHistoryGuard historyGuard;

    public RegisterCommandService(RegisterRepository registerRepository,
                                  RegisterPeripheralRepository peripheralRepository,
                                  RegistersMapper mapper, AuditService auditService,
                                  OutboxPublisher outbox, RegisterHistoryGuard historyGuard) {
        this.registerRepository = registerRepository;
        this.peripheralRepository = peripheralRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outbox = outbox;
        this.historyGuard = historyGuard;
    }

    /** AC-005.1-1 / FR-25.1 — create a register, optionally binding peripherals in the same call. */
    @Transactional
    public RegisterResponse create(RegisterCreateRequest req) {
        if (registerRepository.existsByLocationIdAndCode(req.locationId(), req.code())) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "A register with code '" + req.code() + "' already exists at this location");
        }
        Register r = new Register();
        r.setLocationId(req.locationId());
        r.setCode(req.code());
        r.setDisplayName(req.displayName().strip());
        r.setOperatingMode(req.operatingMode() == null
            ? RegisterOperatingMode.FULL_SERVICE : RegisterOperatingMode.valueOf(req.operatingMode()));
        r.setStatus(RegisterStatus.ACTIVE);
        if (req.peripherals() != null) {
            for (PeripheralPayload spec : req.peripherals()) {
                r.addPeripheral(buildPeripheral(spec.type(), spec.connection(), spec.config(), spec.enabled()));
            }
        }
        Register saved = registerRepository.save(r);
        RegisterResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_REGISTER, saved.getPublicId(), AuditAction.CREATE, null, after);
        outbox.publish(AUDIT_REGISTER, saved.getPublicId(), EVENT_REGISTER_CREATED, after);
        return after;
    }

    /** FR-25.6 / FR-25.9 — partial update; a status change to INACTIVE audits as a deactivation. */
    @Transactional
    public RegisterResponse update(String publicId, RegisterUpdateRequest req) {
        Register r = load(publicId);
        checkVersion(r, req.version());
        RegisterResponse before = mapper.toResponse(r);

        if (req.code() != null && !req.code().equals(r.getCode())) {
            if (registerRepository.existsByLocationIdAndCodeAndPublicIdNot(r.getLocationId(), req.code(), publicId)) {
                throw new DomainException(ErrorCode.DUPLICATE,
                    "A register with code '" + req.code() + "' already exists at this location");
            }
            r.setCode(req.code());
        }
        if (req.displayName() != null) {
            r.setDisplayName(req.displayName().strip());
        }
        if (req.operatingMode() != null) {
            r.setOperatingMode(RegisterOperatingMode.valueOf(req.operatingMode()));
        }
        boolean deactivating = req.status() != null
            && RegisterStatus.fromWire(req.status().toLowerCase()) == RegisterStatus.INACTIVE
            && r.getStatus() != RegisterStatus.INACTIVE;
        if (req.status() != null) {
            r.setStatus(RegisterStatus.fromWire(req.status().toLowerCase()));
        }

        Register saved = registerRepository.save(r);
        RegisterResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_REGISTER, saved.getPublicId(), AuditAction.UPDATE, before, after);
        outbox.publish(AUDIT_REGISTER, saved.getPublicId(),
            deactivating ? EVENT_REGISTER_DEACTIVATED : EVENT_REGISTER_UPDATED, after);
        return after;
    }

    /** FR-25.9 / AC-005.1-7 — hard delete blocked when any historical till/transaction exists. */
    @Transactional
    public void delete(String publicId) {
        Register r = load(publicId);
        if (historyGuard.hasHistory(r.getId())) {
            throw new DomainException(ErrorCode.REFERENCED,
                "Register '" + publicId + "' has till or transaction history and cannot be deleted");
        }
        RegisterResponse before = mapper.toResponse(r);
        r.softDelete();
        registerRepository.save(r);
        auditService.record(AUDIT_REGISTER, publicId, AuditAction.DELETE, before, null);
    }

    /** POST .../peripherals — bind (or replace-in-place) a peripheral by type. */
    @Transactional
    public PeripheralResponse bindPeripheral(String registerPublicId, PeripheralBindRequest req) {
        Register r = load(registerPublicId);
        PeripheralType type = PeripheralType.valueOf(req.type());
        RegisterPeripheral existing = peripheralRepository
            .findByRegisterIdAndType(r.getId(), type).orElse(null);
        PeripheralResponse before = existing == null ? null : mapper.toResponse(existing);

        RegisterPeripheral p;
        if (existing != null) {
            existing.setConnection(PeripheralConnection.valueOf(req.connection()));
            existing.setConfig(req.config());
            existing.setEnabled(req.enabled());
            p = peripheralRepository.save(existing);
        } else {
            p = buildPeripheral(req.type(), req.connection(), req.config(), req.enabled());
            r.addPeripheral(p);
            registerRepository.save(r);
            p = peripheralRepository.findByRegisterIdAndType(r.getId(), type)
                .orElseThrow(() -> DomainException.notFound("RegisterPeripheral", type.name()));
        }

        PeripheralResponse after = mapper.toResponse(p);
        auditService.record(AUDIT_PERIPHERAL, p.getPublicId(),
            existing == null ? AuditAction.CREATE : AuditAction.UPDATE, before, after);
        outbox.publish(AUDIT_PERIPHERAL, p.getPublicId(),
            existing == null ? EVENT_PERIPHERAL_BOUND : EVENT_PERIPHERAL_UPDATED, after);
        return after;
    }

    /** FR-25.8 / AC-005.1-6 — config / connection edits write a before+after audit row. */
    @Transactional
    public PeripheralResponse updatePeripheral(String registerPublicId, String peripheralPublicId,
                                               PeripheralUpdateRequest req) {
        Register r = load(registerPublicId);
        RegisterPeripheral p = peripheralRepository.findByRegisterIdAndPublicId(r.getId(), peripheralPublicId)
            .orElseThrow(() -> DomainException.notFound("RegisterPeripheral", peripheralPublicId));
        PeripheralResponse before = mapper.toResponse(p);

        if (req.connection() != null) {
            p.setConnection(PeripheralConnection.valueOf(req.connection()));
        }
        if (req.config() != null) {
            p.setConfig(req.config());
        }
        if (req.enabled() != null) {
            p.setEnabled(req.enabled());
        }
        RegisterPeripheral saved = peripheralRepository.save(p);
        PeripheralResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_PERIPHERAL, saved.getPublicId(), AuditAction.UPDATE, before, after);
        outbox.publish(AUDIT_PERIPHERAL, saved.getPublicId(), EVENT_PERIPHERAL_UPDATED, after);
        return after;
    }

    // --- internals -----------------------------------------------------------

    private RegisterPeripheral buildPeripheral(String type, String connection,
                                               java.util.Map<String, Object> config, boolean enabled) {
        RegisterPeripheral p = new RegisterPeripheral();
        p.setType(PeripheralType.valueOf(type));
        p.setConnection(PeripheralConnection.valueOf(connection));
        p.setConfig(config);
        p.setEnabled(enabled);
        return p;
    }

    Register load(String publicId) {
        return registerRepository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Register", publicId));
    }

    private void checkVersion(Register r, Long requestVersion) {
        if (requestVersion != null && requestVersion != r.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}
