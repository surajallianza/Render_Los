package com.los.bank.customer.service;

import com.los.bank.customer.config.CustomerLookupProperties;
import com.los.bank.lookup.cache.LookupCacheManager;
import com.los.bank.lookup.dto.LookupValue;
import com.los.bank.lookup.entity.Lookup;
import com.los.bank.lookup.entity.SubLookup;
import com.los.bank.lookup.repository.LookupRepository;
import com.los.bank.lookup.repository.SubLookupRepository;
import com.los.bank.lookup.resolver.LookupResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper service for dynamic lookup resolution and validation in customer workflows.
 * Dynamically resolves lookup categories from the database (m001) by category name
 * without requiring hardcoded parent codes.
 */
@Service
public class CustomerLookupHelper {

    private static final Logger logger = LoggerFactory.getLogger(CustomerLookupHelper.class);

    private final CustomerLookupProperties properties;
    private final LookupResolver lookupResolver;
    private final SubLookupRepository subLookupRepository;
    private final LookupRepository lookupRepository;
    private final LookupCacheManager cacheManager;

    public CustomerLookupHelper(CustomerLookupProperties properties,
                                LookupResolver lookupResolver,
                                SubLookupRepository subLookupRepository,
                                LookupRepository lookupRepository,
                                LookupCacheManager cacheManager) {
        this.properties = properties;
        this.lookupResolver = lookupResolver;
        this.subLookupRepository = subLookupRepository;
        this.lookupRepository = lookupRepository;
        this.cacheManager = cacheManager;
    }

    public CustomerLookupProperties getProperties() {
        return properties;
    }

    /**
     * Resolves a sub lookup code dynamically by category name without needing a hardcoded parent code.
     */
    public Long resolveId(String lookupName, String subCode, String fieldKey, Map<String, String> errors) {
        return resolveId(lookupName, null, subCode, fieldKey, errors);
    }

    /**
     * Resolves a sub lookup code against the parent lookup.
     * If parentCode is null or empty, dynamically resolves it from m001 by category name.
     */
    public Long resolveId(String lookupName, String parentCode, String subCode, String fieldKey, Map<String, String> errors) {
        if (subCode == null || subCode.trim().isEmpty()) {
            return null;
        }

        String effectiveParentCode = parentCode;
        if (effectiveParentCode == null || effectiveParentCode.trim().isEmpty()) {
            effectiveParentCode = resolveParentCodeByName(lookupName);
        }

        if (effectiveParentCode == null || effectiveParentCode.trim().isEmpty()) {
            errors.put(fieldKey, "Lookup category '" + lookupName + "' is not configured. Please create the '" + lookupName + "' lookup in Lookup Master first.");
            return null;
        }

        try {
            if (lookupResolver.isValid(effectiveParentCode, subCode.trim())) {
                return lookupResolver.resolveId(effectiveParentCode, subCode.trim());
            } else {
                errors.put(fieldKey, String.format("Invalid or inactive value '%s' for lookup %s", subCode, lookupName));
                return null;
            }
        } catch (Exception e) {
            errors.put(fieldKey, String.format("Invalid or inactive value '%s' for lookup %s", subCode, lookupName));
            return null;
        }
    }

    /**
     * Dynamically discovers the parent lookup code from m001 by category name/description.
     * Uses in-memory cache to guarantee fast sub-millisecond lookups.
     */
    public String resolveParentCodeByName(String lookupName) {
        if (lookupName == null || lookupName.trim().isEmpty()) {
            return null;
        }

        // 1. Check optional override in properties if explicitly provided in environment
        String configuredCode = getConfiguredCode(lookupName);
        if (configuredCode != null && !configuredCode.trim().isEmpty()) {
            return configuredCode.trim();
        }

        // 2. Check thread-safe cache
        String normalizedKey = lookupName.trim().toLowerCase();
        String cached = cacheManager.getParentCode(normalizedKey);
        if (cached != null) {
            return cached;
        }

        // 3. Dynamic lookup from m001 database table by category description or code
        try {
            List<Lookup> lookups = lookupRepository.findByNormalizedName(lookupName.trim());
            Lookup found = lookups.stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                    .findFirst()
                    .orElse(lookups.isEmpty() ? null : lookups.get(0));

            if (found != null && found.getCode() != null) {
                String code = found.getCode().trim();
                cacheManager.putParentCode(normalizedKey, code);
                logger.debug("Dynamically resolved parent lookup '{}' to code '{}'", lookupName, code);
                return code;
            }
        } catch (Exception e) {
            logger.warn("Error querying database for lookup '{}': {}", lookupName, e.getMessage());
        }

        logger.warn("Dynamic lookup resolution failed: No lookup category found matching '{}' in m001", lookupName);
        return null;
    }

    private String getConfiguredCode(String lookupName) {
        if (properties == null || lookupName == null) {
            return null;
        }
        String lower = lookupName.trim().toLowerCase();
        return switch (lower) {
            case "title" -> properties.getTitle();
            case "gender" -> properties.getGender();
            case "marital status", "maritalstatus" -> properties.getMaritalStatus();
            case "customer type", "customertype" -> properties.getCustomerType();
            case "kyc status", "kycstatus" -> properties.getKycStatus();
            case "residential status", "residentialstatus" -> properties.getResidentialStatus();
            case "residential type", "residentialtype" -> properties.getResidentialType();
            case "state" -> properties.getState();
            case "employment type", "employmenttype" -> properties.getEmploymentType();
            case "employer type", "employertype" -> properties.getEmployerType();
            case "listed status", "listedstatus" -> properties.getListedStatus();
            case "income tax filer status", "incometaxfilerstatus" -> properties.getIncomeTaxFilerStatus();
            case "risk category", "riskcategory" -> properties.getRiskCategory();
            case "bank account type", "bankaccounttype" -> properties.getBankAccountType();
            case "bank account status", "bankaccountstatus" -> properties.getBankAccountStatus();
            case "document type", "documenttype" -> properties.getDocumentType();
            default -> null;
        };
    }

    /**
     * Resolves SubLookup ID to LookupValue {code, name} for response rendering.
     */
    public LookupValue resolveLookupValue(Long subLookupId) {
        if (subLookupId == null) {
            return null;
        }
        Optional<SubLookup> subLookup = subLookupRepository.findById(subLookupId);
        return subLookup.map(sub -> LookupValue.of(sub.getSubTypeCode(), sub.getSubTypeDescription())).orElse(null);
    }

    /**
     * Derives gender sub-lookup code from title if gender is not provided.
     * Mr -> Male, Ms / Mrs -> Female.
     */
    public String deriveGenderCodeFromTitleId(Long titleLookupId) {
        if (titleLookupId == null) {
            return null;
        }
        Optional<SubLookup> titleOpt = subLookupRepository.findById(titleLookupId);
        if (titleOpt.isEmpty()) {
            return null;
        }

        String titleDesc = titleOpt.get().getSubTypeDescription();
        if (titleDesc == null) {
            return null;
        }
        String cleanTitle = titleDesc.trim().replace(".", "");

        String genderParentCode = resolveParentCodeByName("Gender");
        if (genderParentCode == null || genderParentCode.trim().isEmpty()) {
            return null;
        }

        List<SubLookup> genderOptions = subLookupRepository.findByLookupTypeCodeAndIsActiveTrueOrderByDisplayOrderAscSubTypeCodeAsc(genderParentCode);

        if ("Mr".equalsIgnoreCase(cleanTitle)) {
            return genderOptions.stream()
                    .filter(g -> g.getSubTypeDescription() != null && g.getSubTypeDescription().equalsIgnoreCase("Male"))
                    .map(SubLookup::getSubTypeCode)
                    .findFirst()
                    .orElse(null);
        } else if ("Ms".equalsIgnoreCase(cleanTitle) || "Mrs".equalsIgnoreCase(cleanTitle)) {
            return genderOptions.stream()
                    .filter(g -> g.getSubTypeDescription() != null && g.getSubTypeDescription().equalsIgnoreCase("Female"))
                    .map(SubLookup::getSubTypeCode)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    /**
     * Checks if a resolved sub lookup description equals target (case-insensitive).
     */
    public boolean descriptionMatches(Long subLookupId, String target) {
        if (subLookupId == null || target == null) {
            return false;
        }
        return subLookupRepository.findById(subLookupId)
                .map(sub -> target.equalsIgnoreCase(sub.getSubTypeDescription()))
                .orElse(false);
    }
}
