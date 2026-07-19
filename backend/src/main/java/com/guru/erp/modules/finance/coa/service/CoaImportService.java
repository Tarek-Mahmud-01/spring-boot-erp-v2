package com.guru.erp.modules.finance.coa.service;

import com.guru.erp.modules.finance.coa.domain.Account;
import com.guru.erp.modules.finance.coa.domain.AccountPostingType;
import com.guru.erp.modules.finance.coa.domain.AccountStatus;
import com.guru.erp.modules.finance.coa.domain.AccountType;
import com.guru.erp.modules.finance.coa.dto.AccountDtos.CoaImportRequest;
import com.guru.erp.modules.finance.coa.dto.AccountDtos.CoaImportResponse;
import com.guru.erp.modules.finance.coa.dto.AccountDtos.CoaImportRowResult;
import com.guru.erp.modules.finance.coa.repository.AccountRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-227 — bulk CSV import of the chart of accounts (reference
 * {@code ChartOfAccountsView.import_chart_of_accounts}). Columns:
 * {@code code,name,type,parent_code,posting_type,currency}.
 *
 * <p>Split out of {@link AccountService} to keep each service focused and
 * under the size cap — this is exactly the kind of module the architecture
 * says to split generously.
 *
 * <p>Unlike single-row create/update/delete (which use
 * {@link NestedSetService}'s incremental shifts), a bulk import rebuilds the
 * WHOLE company's lft/rgt/depth in one iterative pre-order DFS pass after all
 * rows are inserted — exactly like the reference's own
 * {@code chart_of_accounts._rebuild_nested_set}: "for bulk loads (CSV import,
 * seed scripts) a one-shot DFS rebuild is still faster — use that there. The
 * incremental ops are for single-row CRUD."
 */
@Service
public class CoaImportService {

    private static final String AUDIT_ENTITY = "account";
    private static final Set<String> REQUIRED_COLUMNS = Set.of("code", "name", "type");

    private final AccountRepository repository;
    private final AuditService auditService;

    public CoaImportService(AccountRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public CoaImportResponse importCsv(CoaImportRequest req) {
        List<String[]> lines = parseCsv(req.csv());
        if (lines.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "CSV is empty.");
        }
        String[] header = Arrays.stream(lines.get(0))
            .map(h -> h.trim().toLowerCase(Locale.ROOT))
            .toArray(String[]::new);
        Set<String> headerSet = new HashSet<>(Arrays.asList(header));
        if (!headerSet.containsAll(REQUIRED_COLUMNS)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "CSV is missing required columns: " + REQUIRED_COLUMNS);
        }

        Set<String> validTypes = new HashSet<>();
        for (AccountType t : AccountType.values()) {
            validTypes.add(t.name());
        }
        Set<String> validPosting = new HashSet<>();
        for (AccountPostingType p : AccountPostingType.values()) {
            validPosting.add(p.name());
        }

        // code -> posting_type for BOTH already-persisted rows and accepted-
        // but-not-yet-flushed rows in this batch, so the parent-must-be-HEADER
        // check covers forward references too (mirrors the reference).
        Map<String, String> existingCodeKind = new HashMap<>();
        for (Account a : repository.findAllByCompanyId(req.companyId())) {
            existingCodeKind.put(a.getCode(), a.getPostingType().name());
        }
        Set<String> existingCodes = new HashSet<>(existingCodeKind.keySet());

        List<CoaImportRowResult> rows = new ArrayList<>();
        Set<String> acceptedCodes = new HashSet<>();
        Map<String, String> acceptedKind = new HashMap<>();
        List<Map<String, String>> acceptedPayloads = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            int lineNo = i + 1; // header is line 1
            Map<String, String> raw = toRowMap(header, lines.get(i));

            String code = orEmpty(raw.get("code")).trim();
            String name = orEmpty(raw.get("name")).trim();
            String type = orEmpty(raw.get("type")).trim().toUpperCase(Locale.ROOT);
            String parentCode = orEmpty(raw.get("parent_code")).trim();
            String postingType = orEmpty(raw.getOrDefault("posting_type", "POSTING")).trim().toUpperCase(Locale.ROOT);
            if (postingType.isEmpty()) {
                postingType = "POSTING";
            }
            String currency = orEmpty(raw.get("currency")).trim().toUpperCase(Locale.ROOT);
            String currencyOrNull = currency.isEmpty() ? null : currency;

            if (code.isEmpty() || name.isEmpty()) {
                rows.add(new CoaImportRowResult(lineNo, code.isEmpty() ? null : code, false,
                    "code and name are required"));
                continue;
            }
            if (!validTypes.contains(type)) {
                rows.add(new CoaImportRowResult(lineNo, code, false, "invalid type '" + type + "'"));
                continue;
            }
            if (!validPosting.contains(postingType)) {
                rows.add(new CoaImportRowResult(lineNo, code, false, "invalid posting_type '" + postingType + "'"));
                continue;
            }
            if (existingCodes.contains(code) || acceptedCodes.contains(code)) {
                rows.add(new CoaImportRowResult(lineNo, code, false, "duplicate code"));
                continue;
            }
            if (!parentCode.isEmpty()) {
                if (!existingCodes.contains(parentCode) && !acceptedCodes.contains(parentCode)) {
                    rows.add(new CoaImportRowResult(lineNo, code, false, "unknown parent_code '" + parentCode + "'"));
                    continue;
                }
                String parentKind = acceptedKind.getOrDefault(parentCode, existingCodeKind.get(parentCode));
                if (!AccountPostingType.HEADER.name().equals(parentKind)) {
                    rows.add(new CoaImportRowResult(lineNo, code, false,
                        "parent_code '" + parentCode + "' must be a HEADER account"));
                    continue;
                }
            }

            acceptedCodes.add(code);
            acceptedKind.put(code, postingType);
            Map<String, String> spec = new LinkedHashMap<>();
            spec.put("code", code);
            spec.put("name", name);
            spec.put("type", type);
            spec.put("parent_code", parentCode.isEmpty() ? null : parentCode);
            spec.put("posting_type", postingType);
            spec.put("currency", currencyOrNull);
            acceptedPayloads.add(spec);
            rows.add(new CoaImportRowResult(lineNo, code, true, null));
        }

        if (req.dryRun()) {
            return new CoaImportResponse(acceptedPayloads.size(), rows.size() - acceptedPayloads.size(), rows);
        }

        // Two-pass insert so forward parent_code references resolve.
        Map<String, Account> codeToAccount = new HashMap<>();
        List<Account> pending = new ArrayList<>();
        for (Map<String, String> spec : acceptedPayloads) {
            Account a = new Account();
            a.setCompanyId(req.companyId());
            a.setCode(spec.get("code"));
            a.setName(spec.get("name"));
            a.setType(AccountType.valueOf(spec.get("type")));
            a.setPostingType(AccountPostingType.valueOf(spec.get("posting_type")));
            a.setCurrency(spec.get("currency"));
            a.setStatus(AccountStatus.ACTIVE);
            pending.add(a);
            codeToAccount.put(spec.get("code"), a);
        }
        List<Account> saved = repository.saveAll(pending);
        for (int i = 0; i < saved.size(); i++) {
            codeToAccount.put(acceptedPayloads.get(i).get("code"), saved.get(i));
        }

        for (Map<String, String> spec : acceptedPayloads) {
            String parentCode = spec.get("parent_code");
            if (parentCode != null) {
                Account parent = codeToAccount.get(parentCode);
                if (parent == null) {
                    parent = repository.findByCompanyIdAndCodeAndDeletedAtIsNull(req.companyId(), parentCode)
                        .orElse(null);
                }
                if (parent != null) {
                    codeToAccount.get(spec.get("code")).setParent(parent);
                }
            }
        }
        repository.saveAll(codeToAccount.values());
        repository.flush();

        rebuildNestedSet(req.companyId());

        auditService.record(AUDIT_ENTITY, null, AuditAction.CREATE, null, Map.of(
            "import", true,
            "companyId", req.companyId(),
            "accepted", acceptedPayloads.size(),
            "skipped", rows.size() - acceptedPayloads.size()));

        return new CoaImportResponse(acceptedPayloads.size(), rows.size() - acceptedPayloads.size(), rows);
    }

    /**
     * Recompute lft/rgt/depth for ALL accounts of one company via an iterative
     * pre-order DFS (avoids recursion-depth issues on very wide/deep trees),
     * flushed in the caller's transaction — faithful port of the reference's
     * {@code _rebuild_nested_set}. ONLY used after a bulk import; single-row
     * CRUD always goes through {@link NestedSetService}'s incremental shifts.
     */
    private void rebuildNestedSet(String companyId) {
        List<Account> accounts = repository.findAllByCompanyId(companyId);

        Map<Long, List<Account>> childrenByParent = new HashMap<>();
        List<Account> roots = new ArrayList<>();
        for (Account a : accounts) {
            if (a.getParent() == null) {
                roots.add(a);
            } else {
                childrenByParent.computeIfAbsent(a.getParent().getId(), k -> new ArrayList<>()).add(a);
            }
        }
        roots.sort((a, b) -> a.getCode().compareTo(b.getCode()));
        for (List<Account> siblings : childrenByParent.values()) {
            siblings.sort((a, b) -> a.getCode().compareTo(b.getCode()));
        }

        record Frame(Account node, int depth, boolean entered) {
        }
        Deque<Frame> stack = new ArrayDeque<>();
        for (int i = roots.size() - 1; i >= 0; i--) {
            stack.push(new Frame(roots.get(i), 0, false));
        }

        int counter = 0;
        while (!stack.isEmpty()) {
            Frame frame = stack.pop();
            if (!frame.entered()) {
                counter++;
                frame.node().setLft(counter);
                frame.node().setDepth(frame.depth());
                stack.push(new Frame(frame.node(), frame.depth(), true));
                List<Account> children = childrenByParent.getOrDefault(frame.node().getId(), List.of());
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(new Frame(children.get(i), frame.depth() + 1, false));
                }
            } else {
                counter++;
                frame.node().setRgt(counter);
            }
        }
        repository.saveAll(accounts);
        repository.flush();
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static Map<String, String> toRowMap(String[] header, String[] values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            row.put(header[i], i < values.length ? values[i] : "");
        }
        return row;
    }

    /** Minimal RFC-4180 CSV parser: comma-separated, double-quote escaping, no embedded newlines in quotes handling beyond basic support. */
    private static List<String[]> parseCsv(String csv) {
        List<String[]> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csv))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                result.add(splitCsvLine(line));
            }
        } catch (IOException e) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "Failed to parse CSV: " + e.getMessage());
        }
        return result;
    }

    private static String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
