package com.los.bank.rule.service;

import com.los.bank.lookup.entity.Lookup;
import com.los.bank.lookup.entity.SubLookup;
import com.los.bank.lookup.repository.LookupRepository;
import com.los.bank.lookup.repository.SubLookupRepository;
import com.los.bank.rule.config.RuleLookupProperties;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * In-memory Lookup Master for rule tests: Rule Parameter (10020), Rule Operator (10021),
 * Rule Decision (10022) and the Firm Registration choice lookup (10023).
 */
public final class RuleTestLookups {

    public final LookupRepository lookupRepository = mock(LookupRepository.class);
    public final SubLookupRepository subLookupRepository = mock(SubLookupRepository.class);
    public final RuleLookupService service;

    private final List<Lookup> lookups = new ArrayList<>();
    private final Map<String, List<SubLookup>> subsByType = new HashMap<>();
    private final Map<Long, SubLookup> subsById = new HashMap<>();
    private final Map<String, SubLookup> subsByName = new HashMap<>();
    private long nextId = 100;

    public RuleTestLookups() {
        add("10020", "Rule Parameter", "FOIR", "Debt to Income (DTI)", "Loan-to-Value (LTV)", "CIBIL Score", "Age", "Firm Registration");
        add("10021", "Rule Operator", "Below", "Between", "Above", "Is");
        add("10022", "Rule Decision", "Allow", "Override", "Reject");
        add("10023", "Firm Registration", "Registered", "Not Registered");

        when(lookupRepository.findByNormalizedName(anyString())).thenAnswer(inv -> {
            String key = norm(inv.getArgument(0));
            return lookups.stream().filter(l -> norm(l.getDescription()).equals(key) || l.getCode().equals(inv.getArgument(0))).toList();
        });
        when(subLookupRepository.findByLookupTypeCodeAndIsActiveTrueOrderByDisplayOrderAscSubTypeCodeAsc(anyString()))
                .thenAnswer(inv -> subsByType.getOrDefault(inv.<String>getArgument(0), List.of()));
        when(subLookupRepository.findAllById(any())).thenAnswer(inv -> {
            List<SubLookup> found = new ArrayList<>();
            for (Object id : (Iterable<?>) inv.getArgument(0)) {
                SubLookup s = subsById.get((Long) id);
                if (s != null) {
                    found.add(s);
                }
            }
            return found;
        });
        service = new RuleLookupService(lookupRepository, subLookupRepository, new RuleLookupProperties());
    }

    private void add(String code, String name, String... values) {
        lookups.add(Lookup.builder().id((long) lookups.size() + 1).code(code).description(name).isActive(true).build());
        List<SubLookup> subs = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            SubLookup sub = SubLookup.builder().id(nextId++).lookupTypeCode(code)
                    .subTypeCode(code + String.format("%02d", i + 1)).subTypeDescription(values[i])
                    .isActive(true).displayOrder(i + 1).build();
            subs.add(sub);
            subsById.put(sub.getId(), sub);
            subsByName.putIfAbsent(values[i], sub);
        }
        subsByType.put(code, subs);
    }

    /** The sub lookup with this name (first lookup that has it). */
    public SubLookup sub(String name) {
        return subsByName.get(name);
    }

    public Long id(String name) {
        return sub(name).getId();
    }

    public String code(String name) {
        return sub(name).getSubTypeCode();
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replace(" ", "");
    }
}
