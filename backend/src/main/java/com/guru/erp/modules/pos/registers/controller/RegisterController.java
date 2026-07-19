package com.guru.erp.modules.pos.registers.controller;

import com.guru.erp.modules.pos.registers.dto.RegisterDtos.PeripheralBindRequest;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.PeripheralResponse;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.PeripheralTestResult;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.PeripheralUpdateRequest;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.RegisterCreateRequest;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.RegisterResponse;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.RegisterUpdateRequest;
import com.guru.erp.modules.pos.registers.service.PeripheralTestService;
import com.guru.erp.modules.pos.registers.service.RegisterCommandService;
import com.guru.erp.modules.pos.registers.service.RegisterQueryService;
import com.guru.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-060 Register + ENT-060a RegisterPeripheral endpoints (US-005.1) — thin
 * controller, {@code @PreAuthorize} per method. Business rules live in the
 * command/query services.
 */
@RestController
@RequestMapping("/api/pos/registers")
public class RegisterController {

    private final RegisterCommandService command;
    private final RegisterQueryService query;
    private final PeripheralTestService peripheralTest;

    public RegisterController(RegisterCommandService command, RegisterQueryService query,
                              PeripheralTestService peripheralTest) {
        this.command = command;
        this.query = query;
        this.peripheralTest = peripheralTest;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('pos.register.read')")
    public PageResponse<RegisterResponse> list(@RequestParam(required = false) String locationId,
                                               @PageableDefault(size = 50) Pageable pageable) {
        return query.list(locationId, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pos.register.read')")
    public RegisterResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('pos.register.write')")
    public RegisterResponse create(@Valid @RequestBody RegisterCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pos.register.write')")
    public RegisterResponse update(@PathVariable String publicId,
                                   @Valid @RequestBody RegisterUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('pos.register.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** FR-25.2..25.5 — bind (or replace-in-place) a peripheral by type. */
    @PostMapping("/{publicId}/peripherals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('pos.register.write')")
    public PeripheralResponse bindPeripheral(@PathVariable String publicId,
                                             @Valid @RequestBody PeripheralBindRequest request) {
        return command.bindPeripheral(publicId, request);
    }

    /** FR-25.8 / AC-005.1-6 — edit a bound peripheral's connection/config/enabled. */
    @PatchMapping("/{publicId}/peripherals/{peripheralId}")
    @PreAuthorize("hasAuthority('pos.register.write')")
    public PeripheralResponse updatePeripheral(@PathVariable String publicId,
                                               @PathVariable String peripheralId,
                                               @Valid @RequestBody PeripheralUpdateRequest request) {
        return command.updatePeripheral(publicId, peripheralId, request);
    }

    /** FR-25.2 — Test Print. Config is never mutated on failure (AC-005.1-2). */
    @PostMapping("/{publicId}/peripherals/{peripheralId}/test-print")
    @PreAuthorize("hasAuthority('pos.register.write')")
    public PeripheralTestResult testPrint(@PathVariable String publicId, @PathVariable String peripheralId) {
        return peripheralTest.test(publicId, peripheralId, PeripheralTestService.ACTION_TEST_PRINT);
    }

    /** FR-25.3 — Test Open (cash drawer). Config is never mutated on failure (AC-005.1-2). */
    @PostMapping("/{publicId}/peripherals/{peripheralId}/test-open")
    @PreAuthorize("hasAuthority('pos.register.write')")
    public PeripheralTestResult testOpen(@PathVariable String publicId, @PathVariable String peripheralId) {
        return peripheralTest.test(publicId, peripheralId, PeripheralTestService.ACTION_TEST_OPEN);
    }
}
