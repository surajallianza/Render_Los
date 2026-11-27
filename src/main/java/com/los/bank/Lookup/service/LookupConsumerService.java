package com.los.bank.lookup.service;

import com.los.bank.lookup.cache.LookupCacheManager;
import com.los.bank.lookup.dto.LookupDropdownItem;
import com.los.bank.lookup.entity.Lookup;
import com.los.bank.lookup.entity.SubLookup;
import com.los.bank.lookup.repository.LookupRepository;
import com.los.bank.lookup.repository.SubLookupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service powering form consumer dropdowns and bulk lookup resolution.
 */
@Service
@Transactional(transactionManager = "bankTransactionManager")
public class LookupConsumerService {

    private final LookupRepository lookupRepository;
    private final SubLookupRepository subLookupRepository;
    private final LookupCacheManager cacheManager;

    public LookupConsumerService(LookupRepository lookupRepository,
                                 SubLookupRepository subLookupRepository,
                                 LookupCacheManager cacheManager) {
        this.lookupRepository = lookupRepository;
        this.subLookupRepository = subLookupRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Retrieves dropdown options for a single parent lookup code.
     * Returns empty list if lookup does not exist or is inactive.
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public List<LookupDropdownItem> getDropdownOptions(String lookupCode, boolean activeOnly) {
        if (lookupCode == null || lookupCode.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normCode = lookupCode.trim();

        // Check cache
        List<LookupDropdownItem> cached = cacheManager.getDropdown(normCode, activeOnly);
        if (cached != null) {
            return cached;
        }

        Lookup parent = lookupRepository.findByCode(normCode).orElse(null);
        if (parent == null || (activeOnly && !Boolean.TRUE.equals(parent.getIsActive()))) {
            cacheManager.putDropdown(normCode, activeOnly, Collections.emptyList());
            return Collections.emptyList();
        }

        List<SubLookup> subLookups;
        if (activeOnly) {
            subLookups = subLookupRepository.findByLookupTypeCodeAndIsActiveTrueOrderByDisplayOrderAscSubTypeCodeAsc(normCode);
        } else {
            subLookups = subLookupRepository.findByLookupTypeCodeOrderByDisplayOrderAscSubTypeCodeAsc(normCode);
        }

        List<LookupDropdownItem> items = subLookups.stream()
                .map(s -> LookupDropdownItem.builder()
                        .code(s.getSubTypeCode())
                        .subLookupCode(s.getSubTypeCode())
                        .name(s.getSubTypeDescription())
                        .displayOrder(s.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        cacheManager.putDropdown(normCode, activeOnly, items);
        return items;
    }

    /**
     * Retrieves bulk dropdown options for multiple parent lookup codes in a single request.
     * Returns a map of lookupCode -> list of dropdown items.
     * Unknown codes return an empty list.
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public Map<String, List<LookupDropdownItem>> getBulkDropdownOptions(Collection<String> lookupCodes, boolean activeOnly) {
        if (lookupCodes == null || lookupCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<LookupDropdownItem>> resultMap = new LinkedHashMap<>();
        for (String code : lookupCodes) {
            if (code != null && !code.trim().isEmpty()) {
                String trimmed = code.trim();
                resultMap.put(trimmed, getDropdownOptions(trimmed, activeOnly));
            }
        }

        return resultMap;
    }
}
