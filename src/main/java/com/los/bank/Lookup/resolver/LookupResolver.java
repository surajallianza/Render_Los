package com.los.bank.lookup.resolver;

import com.los.bank.lookup.cache.LookupCacheManager;
import com.los.bank.lookup.dto.LookupValue;
import com.los.bank.lookup.entity.Lookup;
import com.los.bank.lookup.entity.SubLookup;
import com.los.bank.lookup.exception.LookupBadRequestException;
import com.los.bank.lookup.repository.LookupRepository;
import com.los.bank.lookup.repository.SubLookupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reusable Spring component for resolving and validating lookup codes across all LOS forms
 * (loan application, customer onboarding, etc.).
 * Always resolves by the pair (lookup_type_code, sub_type_code).
 */
@Component
@Transactional(transactionManager = "bankTransactionManager")
public class LookupResolver {

    private static final Logger logger = LoggerFactory.getLogger(LookupResolver.class);

    private final LookupRepository lookupRepository;
    private final SubLookupRepository subLookupRepository;
    private final LookupCacheManager cacheManager;

    public LookupResolver(LookupRepository lookupRepository,
                          SubLookupRepository subLookupRepository,
                          LookupCacheManager cacheManager) {
        this.lookupRepository = lookupRepository;
        this.subLookupRepository = subLookupRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Resolves (lookupCode, subCode) to m002.id.
     * Throws LookupBadRequestException (400) if parent lookup or sub lookup does not exist,
     * is inactive, or the code does not belong to that lookup.
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public Long resolveId(String lookupCode, String subCode) {
        if (lookupCode == null || lookupCode.trim().isEmpty() || subCode == null || subCode.trim().isEmpty()) {
            throw new LookupBadRequestException(String.format("Invalid or inactive value '%s' for lookup %s", subCode, lookupCode));
        }

        String normLookup = lookupCode.trim();
        String normSub = subCode.trim();

        // 1. Check cache
        Long cachedId = cacheManager.getResolvedId(normLookup, normSub);
        if (cachedId != null) {
            return cachedId;
        }

        // 2. Verify parent lookup exists and is active
        Lookup parent = lookupRepository.findByCode(normLookup).orElse(null);
        if (parent == null || !Boolean.TRUE.equals(parent.getIsActive())) {
            logger.warn("Resolution failed: Parent lookup '{}' not found or inactive", normLookup);
            throw new LookupBadRequestException(String.format("Invalid or inactive value '%s' for lookup %s", normSub, normLookup));
        }

        // 3. Verify sub lookup exists, belongs to parent lookup, and is active
        SubLookup subLookup = subLookupRepository.findByLookupTypeCodeAndSubTypeCode(normLookup, normSub).orElse(null);
        if (subLookup == null && !normSub.startsWith(normLookup)) {
            String candidate = normLookup + (normSub.length() == 1 ? "0" + normSub : normSub);
            subLookup = subLookupRepository.findByLookupTypeCodeAndSubTypeCode(normLookup, candidate).orElse(null);
        }
        if (subLookup == null || !Boolean.TRUE.equals(subLookup.getIsActive())) {
            logger.warn("Resolution failed: Sub lookup '{}:{}' not found or inactive", normLookup, normSub);
            throw new LookupBadRequestException(String.format("Invalid or inactive value '%s' for lookup %s", normSub, normLookup));
        }

        // 4. Cache results
        cacheManager.putResolvedId(normLookup, normSub, subLookup.getId());
        cacheManager.putResolvedName(normLookup, normSub, subLookup.getSubTypeDescription());
        cacheManager.putValidity(normLookup, normSub, true);
        if (!normSub.equals(subLookup.getSubTypeCode())) {
            cacheManager.putResolvedId(normLookup, subLookup.getSubTypeCode(), subLookup.getId());
            cacheManager.putResolvedName(normLookup, subLookup.getSubTypeCode(), subLookup.getSubTypeDescription());
            cacheManager.putValidity(normLookup, subLookup.getSubTypeCode(), true);
        }

        return subLookup.getId();
    }

    /**
     * Resolves human-readable description for (lookupCode, subCode).
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public String resolveName(String lookupCode, String subCode) {
        if (lookupCode == null || subCode == null) {
            return null;
        }

        String normLookup = lookupCode.trim();
        String normSub = subCode.trim();

        String cachedName = cacheManager.getResolvedName(normLookup, normSub);
        if (cachedName != null) {
            return cachedName;
        }

        // Ensure resolution validity (will throw 400 if invalid/inactive)
        resolveId(normLookup, normSub);
        return cacheManager.getResolvedName(normLookup, normSub);
    }

    /**
     * Validates whether (lookupCode, subCode) is active and valid.
     * Returns false instead of throwing exception.
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public boolean isValid(String lookupCode, String subCode) {
        if (lookupCode == null || lookupCode.trim().isEmpty() || subCode == null || subCode.trim().isEmpty()) {
            return false;
        }

        String normLookup = lookupCode.trim();
        String normSub = subCode.trim();

        Boolean cachedValid = cacheManager.getValidity(normLookup, normSub);
        if (cachedValid != null) {
            return cachedValid;
        }

        Lookup parent = lookupRepository.findByCode(normLookup).orElse(null);
        if (parent == null || !Boolean.TRUE.equals(parent.getIsActive())) {
            cacheManager.putValidity(normLookup, normSub, false);
            return false;
        }

        SubLookup subLookup = subLookupRepository.findByLookupTypeCodeAndSubTypeCode(normLookup, normSub).orElse(null);
        if (subLookup == null && !normSub.startsWith(normLookup)) {
            String candidate = normLookup + (normSub.length() == 1 ? "0" + normSub : normSub);
            subLookup = subLookupRepository.findByLookupTypeCodeAndSubTypeCode(normLookup, candidate).orElse(null);
        }
        boolean valid = subLookup != null && Boolean.TRUE.equals(subLookup.getIsActive());

        cacheManager.putValidity(normLookup, normSub, valid);
        if (valid) {
            cacheManager.putResolvedId(normLookup, normSub, subLookup.getId());
            cacheManager.putResolvedName(normLookup, normSub, subLookup.getSubTypeDescription());
            if (!normSub.equals(subLookup.getSubTypeCode())) {
                cacheManager.putResolvedId(normLookup, subLookup.getSubTypeCode(), subLookup.getId());
                cacheManager.putResolvedName(normLookup, subLookup.getSubTypeCode(), subLookup.getSubTypeDescription());
                cacheManager.putValidity(normLookup, subLookup.getSubTypeCode(), true);
            }
        }

        return valid;
    }

    /**
     * Batch resolution method for resolving multiple subCodes under a lookupCode in a single query.
     * Prevents N+1 database queries when processing bulk forms.
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public Map<String, Long> resolveBatch(String lookupCode, Collection<String> subCodes) {
        if (lookupCode == null || subCodes == null || subCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        String normLookup = lookupCode.trim();

        // Check if parent lookup is active
        Lookup parent = lookupRepository.findByCode(normLookup).orElse(null);
        if (parent == null || !Boolean.TRUE.equals(parent.getIsActive())) {
            return Collections.emptyMap();
        }

        List<SubLookup> matches = subLookupRepository.findByLookupTypeCodeAndIsActiveTrueOrderByDisplayOrderAscSubTypeCodeAsc(normLookup);
        Set<String> requestedSubCodes = subCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toSet());

        Map<String, Long> resolvedMap = new HashMap<>();
        for (SubLookup sub : matches) {
            if (requestedSubCodes.contains(sub.getSubTypeCode())) {
                resolvedMap.put(sub.getSubTypeCode(), sub.getId());
                cacheManager.putResolvedId(normLookup, sub.getSubTypeCode(), sub.getId());
                cacheManager.putResolvedName(normLookup, sub.getSubTypeCode(), sub.getSubTypeDescription());
                cacheManager.putValidity(normLookup, sub.getSubTypeCode(), true);
            }
        }

        return resolvedMap;
    }

    /**
     * Helper to map a submitted code into a LookupValue response DTO (code + name).
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public LookupValue toLookupValue(String lookupCode, String subCode) {
        if (subCode == null || subCode.trim().isEmpty()) {
            return null;
        }
        String name = resolveName(lookupCode, subCode);
        return LookupValue.of(subCode.trim(), name);
    }
}
