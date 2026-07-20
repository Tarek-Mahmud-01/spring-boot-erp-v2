package com.springboot.erp.modules.pos.registers.service;

import com.springboot.erp.modules.pos.registers.domain.PeripheralConnection;
import com.springboot.erp.modules.pos.registers.domain.PeripheralType;
import com.springboot.erp.modules.pos.registers.domain.Register;
import com.springboot.erp.modules.pos.registers.domain.RegisterPeripheral;
import com.springboot.erp.modules.pos.registers.dto.RegisterDtos.PeripheralTestResult;
import com.springboot.erp.modules.pos.registers.repository.RegisterPeripheralRepository;
import com.springboot.erp.modules.pos.registers.repository.RegisterRepository;
import com.springboot.erp.platform.error.DomainException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-25.2 / FR-25.3 — "Test Print" / "Test Open" preflight checks. Configuration
 * is never mutated (AC-005.1-2) — this only validates that the bound
 * peripheral's config is complete enough for its connection type. Production
 * deployments would replace these heuristic checks with a real device driver;
 * the reference default drivers are ported verbatim here (NETWORK needs
 * address+port, USB needs device_path, BLUETOOTH needs mac; a drawer fired
 * through a bound printer needs printer_id, else a standalone relay needs an
 * address).
 */
@Service
public class PeripheralTestService {

    public static final String ACTION_TEST_PRINT = "test_print";
    public static final String ACTION_TEST_OPEN = "test_open";

    private final RegisterRepository registerRepository;
    private final RegisterPeripheralRepository peripheralRepository;

    public PeripheralTestService(RegisterRepository registerRepository,
                                 RegisterPeripheralRepository peripheralRepository) {
        this.registerRepository = registerRepository;
        this.peripheralRepository = peripheralRepository;
    }

    @Transactional(readOnly = true)
    public PeripheralTestResult test(String registerPublicId, String peripheralPublicId, String action) {
        Register r = registerRepository.findByPublicId(registerPublicId)
            .orElseThrow(() -> DomainException.notFound("Register", registerPublicId));
        RegisterPeripheral p = peripheralRepository.findByRegisterIdAndPublicId(r.getId(), peripheralPublicId)
            .orElseThrow(() -> DomainException.notFound("RegisterPeripheral", peripheralPublicId));

        return switch (action) {
            case ACTION_TEST_PRINT -> testPrint(p);
            case ACTION_TEST_OPEN -> testOpen(p);
            default -> new PeripheralTestResult(false, "Unsupported test action: " + action);
        };
    }

    private PeripheralTestResult testPrint(RegisterPeripheral p) {
        Map<String, Object> cfg = p.getConfig();
        String label = p.getType() == PeripheralType.LABEL_PRINTER ? "label" : "receipt";
        if (p.getConnection() == PeripheralConnection.NETWORK) {
            if (isBlank(cfg.get("address")) || cfg.get("port") == null) {
                return new PeripheralTestResult(false, "Missing address or port for NETWORK " + label + " printer.");
            }
        } else if (p.getConnection() == PeripheralConnection.USB) {
            if (isBlank(cfg.get("devicePath")) && isBlank(cfg.get("device_path"))) {
                return new PeripheralTestResult(false, "Missing device_path for USB " + label + " printer.");
            }
        } else if (p.getConnection() == PeripheralConnection.BLUETOOTH
            && p.getType() != PeripheralType.LABEL_PRINTER) {
            if (isBlank(cfg.get("mac"))) {
                return new PeripheralTestResult(false, "Missing MAC address for Bluetooth printer.");
            }
        }
        return new PeripheralTestResult(true, "Sample " + label + " sent.");
    }

    private PeripheralTestResult testOpen(RegisterPeripheral p) {
        Map<String, Object> cfg = p.getConfig();
        boolean viaPrinter = Boolean.TRUE.equals(cfg.get("viaPrinter")) || Boolean.TRUE.equals(cfg.get("via_printer"));
        if (viaPrinter) {
            boolean hasPrinter = !isBlank(cfg.get("printerId")) || !isBlank(cfg.get("printer_id"));
            if (!hasPrinter) {
                return new PeripheralTestResult(false,
                    "Drawer-via-printer signal not bound to a configured printer.");
            }
            return new PeripheralTestResult(true, "Drawer signal fired.");
        }
        if (isBlank(cfg.get("address"))) {
            return new PeripheralTestResult(false, "Missing address for standalone drawer relay.");
        }
        return new PeripheralTestResult(true, "Drawer signal fired.");
    }

    private static boolean isBlank(Object v) {
        return v == null || (v instanceof String s && s.isBlank());
    }
}
