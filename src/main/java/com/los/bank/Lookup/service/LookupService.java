package com.los.bank.lookup.service;

import com.los.bank.lookup.cache.LookupCacheManager;
import com.los.bank.lookup.dto.*;
import com.los.bank.lookup.entity.Lookup;
import com.los.bank.lookup.entity.SubLookup;
import com.los.bank.lookup.exception.LookupBadRequestException;
import com.los.bank.lookup.exception.LookupConflictException;
import com.los.bank.lookup.exception.LookupNotFoundException;
import com.los.bank.lookup.repository.LookupRepository;
import com.los.bank.lookup.repository.SubLookupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(transactionManager = "bankTransactionManager")
public class LookupService {

    private static final Logger logger = LoggerFactory.getLogger(LookupService.class);
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");

    private final LookupRepository lookupRepository;
    private final SubLookupRepository subLookupRepository;
    private final LookupCacheManager cacheManager;

    public LookupService(LookupRepository lookupRepository,
                         SubLookupRepository subLookupRepository,
                         LookupCacheManager cacheManager) {
        this.lookupRepository = lookupRepository;
        this.subLookupRepository = subLookupRepository;
        this.cacheManager = cacheManager;
    }

    // =====================================================================
    // LOOKUP MANAGEMENT (m001)
    // =====================================================================

    /**
     * Searches lookups by optional search term and status filter.
     * Includes embedded sub lookups for table row expansion in UI.
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public List<LookupResponse> searchLookups(String search, String status) {
        String normalizedStatus = normalizeStatusFilter(status);
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        List<Lookup> lookups = lookupRepository.searchLookupsWithSubLookups(trimmedSearch, normalizedStatus);
        return lookups.stream()
                .map(this::toLookupResponseWithSubLookups)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single lookup by ID.
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public LookupResponse getLookupById(Long id) {
        Lookup lookup = lookupRepository.findById(id)
                .orElseThrow(() -> new LookupNotFoundException("Lookup not found with ID: " + id));
        return toLookupResponseWithSubLookups(lookup);
    }

    /**
     * Creates a new lookup master record.
     */
    @Transactional(transactionManager = "bankTransactionManager")
    public LookupResponse createLookup(LookupCreateRequest request) {
        if (request == null) {
            throw new LookupBadRequestException("Lookup creation request cannot be null");
        }

        String code = request.getCode() != null ? request.getCode().trim() : "";
        validateDigitsOnly(code, "Lookup code");

        if (lookupRepository.existsByCode(code)) {
            throw new LookupConflictException("Lookup with code '" + code + "' already exists");
        }

        String effectiveDescription = request.getEffectiveDescription();
        if (effectiveDescription == null || effectiveDescription.trim().isEmpty()) {
            throw new LookupBadRequestException("Lookup name/description is required");
        }

        boolean isActive = parseStatus(request.getStatus(), true);
        boolean isFixed = Boolean.TRUE.equals(request.getIsFixed());

        Lookup lookup = Lookup.builder()
                .code(code)
                .description(effectiveDescription)
                .isFixed(isFixed)
                .isActive(isActive)
                .createdBy(getCurrentUserId())
                .build();

        Lookup saved = lookupRepository.save(lookup);
        cacheManager.evictLookup(code);

        logger.info("Created lookup '{}' (id={}) by user {}", code, saved.getId(), saved.getCreatedBy());
        return toLookupResponse(saved, Collections.emptyList());
    }

    /**
     * Updates an existing lookup (name, description, status).
     * Disallows deactivating a fixed lookup.
     */
    @Transactional(transactionManager = "bankTransactionManager")
    public LookupResponse updateLookup(Long id, LookupUpdateRequest request) {
        if (request == null) {
            throw new LookupBadRequestException("Update request cannot be null");
        }

        Lookup lookup = lookupRepository.findById(id)
                .orElseThrow(() -> new LookupNotFoundException("Lookup not found with ID: " + id));

        String effectiveDescription = request.getEffectiveDescription();
        if (effectiveDescription != null && !effectiveDescription.trim().isEmpty()) {
            lookup.setDescription(effectiveDescription.trim());
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            boolean newActive = parseStatus(request.getStatus(), lookup.getIsActive());
            if (!newActive && Boolean.TRUE.equals(lookup.getIsFixed())) {
                throw new LookupBadRequestException("Fixed lookup (code: '" + lookup.getCode() + "') cannot be deactivated");
            }
            lookup.setIsActive(newActive);
        }

        lookup.setModifiedBy(getCurrentUserId());
        Lookup saved = lookupRepository.save(lookup);
        cacheManager.evictLookup(lookup.getCode());

        logger.info("Updated lookup '{}' (id={}) by user {}", lookup.getCode(), saved.getId(), saved.getModifiedBy());
        List<SubLookup> subLookups = subLookupRepository.findByLookupTypeCodeOrderByDisplayOrderAscSubTypeCodeAsc(lookup.getCode());
        return toLookupResponse(saved, subLookups);
    }

    /**
     * Toggles status between Active and Inactive for a lookup.
     */
    @Transactional(transactionManager = "bankTransactionManager")
    public LookupResponse toggleLookupStatus(Long id, StatusUpdateRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new LookupBadRequestException("Status update request cannot be null");
        }

        Lookup lookup = lookupRepository.findById(id)
                .orElseThrow(() -> new LookupNotFoundException("Lookup not found with ID: " + id));

        boolean newActive = request.isActive();
        if (!newActive && Boolean.TRUE.equals(lookup.getIsFixed())) {
            throw new LookupBadRequestException("Fixed lookup (code: '" + lookup.getCode() + "') cannot be deactivated");
        }

        lookup.setIsActive(newActive);
        lookup.setModifiedBy(getCurrentUserId());
        Lookup saved = lookupRepository.save(lookup);
        cacheManager.evictLookup(lookup.getCode());

        logger.info("Toggled status of lookup '{}' to {} by user {}", lookup.getCode(), newActive ? "Active" : "Inactive", saved.getModifiedBy());
        List<SubLookup> subLookups = subLookupRepository.findByLookupTypeCodeOrderByDisplayOrderAscSubTypeCodeAsc(lookup.getCode());
        return toLookupResponse(saved, subLookups);
    }

    // =====================================================================
    // SUB LOOKUP MANAGEMENT (m002)
    // =====================================================================

    /**
     * Retrieves all sub lookups belonging to a parent lookup ID.
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public List<SubLookupResponse> getSubLookupsByLookupId(Long lookupId) {
        return getSubLookupsByLookupIdentifier(lookupId != null ? String.valueOf(lookupId) : null);
    }

    /**
     * Retrieves all sub lookups belonging to a parent lookup identifier (ID or code).
     */
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public List<SubLookupResponse> getSubLookupsByLookupIdentifier(String lookupIdentifier) {
        Lookup parent = resolveParentLookup(lookupIdentifier, null);

        List<SubLookup> subLookups = subLookupRepository.findByLookupTypeCodeOrderByDisplayOrderAscSubTypeCodeAsc(parent.getCode());
        return subLookups.stream()
                .map(this::toSubLookupResponse)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new sub lookup under a parent lookup ID.
     */
    @Transactional(transactionManager = "bankTransactionManager")
    public SubLookupResponse createSubLookup(Long lookupId, SubLookupCreateRequest request) {
        return createSubLookup(lookupId != null ? String.valueOf(lookupId) : null, request);
    }

    /**
     * Creates a new sub lookup under a parent lookup identifier (ID or code, or from request body).
     */
    @Transactional(transactionManager = "bankTransactionManager")
    public SubLookupResponse createSubLookup(String lookupIdentifier, SubLookupCreateRequest request) {
        if (request == null) {
            throw new LookupBadRequestException("Sub lookup creation request cannot be null");
        }

        Lookup parent = resolveParentLookup(lookupIdentifier, request);

        String subCode = request.getEffectiveSubLookupCode(parent.getCode());
        if (subCode == null || subCode.trim().isEmpty()) {
            throw new LookupBadRequestException("Sub lookup code ('sub_lookup_code') is required (e.g. '" + parent.getCode() + "01')");
        }

        validateDigitsOnly(subCode, "Sub lookup code");

        // Sub lookup code format must be <parentCode><subCode> (e.g. 1000801 for parent 10008)
        if (!subCode.startsWith(parent.getCode())) {
            throw new LookupBadRequestException(String.format(
                    "Sub lookup code '%s' must start with parent lookup code '%s' (format: %s01)",
                    subCode, parent.getCode(), parent.getCode()));
        }

        if (subLookupRepository.existsByLookupTypeCodeAndSubTypeCode(parent.getCode(), subCode)) {
            throw new LookupConflictException(String.format(
                    "Sub lookup with code '%s' already exists under lookup '%s'", subCode, parent.getCode()));
        }

        String effectiveDescription = request.getEffectiveDescription();
        if (effectiveDescription == null || effectiveDescription.trim().isEmpty()) {
            throw new LookupBadRequestException("Sub lookup name/description is required");
        }

        // Determine display order: if not provided, max + 1
        int displayOrder;
        if (request.getDisplayOrder() != null) {
            displayOrder = request.getDisplayOrder();
        } else {
            Integer maxOrder = subLookupRepository.findMaxDisplayOrderByLookupTypeCode(parent.getCode());
            displayOrder = (maxOrder != null ? maxOrder : 0) + 1;
        }

        boolean isActive = parseStatus(request.getStatus(), true);
        boolean isFixed = Boolean.TRUE.equals(request.getIsFixed());

        SubLookup subLookup = SubLookup.builder()
                .lookupTypeCode(parent.getCode())
                .subTypeCode(subCode)
                .subTypeDescription(effectiveDescription)
                .isFixed(isFixed)
                .isActive(isActive)
                .displayOrder(displayOrder)
                .createdBy(getCurrentUserId())
                .build();

        SubLookup saved = subLookupRepository.save(subLookup);
        cacheManager.evictLookup(parent.getCode());

        logger.info("Created sub lookup '{}:{}' (id={}) by user {}", parent.getCode(), subCode, saved.getId(), saved.getCreatedBy());
        return toSubLookupResponse(saved);
    }

    private Lookup resolveParentLookup(String lookupIdentifier, SubLookupCreateRequest request) {
        // 1. From path variable if present
        if (lookupIdentifier != null && !lookupIdentifier.trim().isEmpty()) {
            String trimmed = lookupIdentifier.trim();
            try {
                Long id = Long.parseLong(trimmed);
                Optional<Lookup> byId = lookupRepository.findById(id);
                if (byId.isPresent()) {
                    return byId.get();
                }
            } catch (NumberFormatException ignored) {}

            return lookupRepository.findByCode(trimmed)
                    .orElseThrow(() -> new LookupNotFoundException("Parent lookup not found with ID or code: " + trimmed));
        }

        // 2. From request body: check lookupCode, then code, then derive from subLookupCode
        if (request != null) {
            String parentRef = null;
            if (request.getLookupCode() != null && !request.getLookupCode().trim().isEmpty()) {
                parentRef = request.getLookupCode().trim();
            } else if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
                parentRef = request.getCode().trim();
            } else if (request.getSubLookupCode() != null && request.getSubLookupCode().trim().length() > 2) {
                // If sub lookup code is e.g. "1000801", derive parent code prefix "10008" (last 2 digits are sub code)
                String raw = request.getSubLookupCode().trim();
                parentRef = raw.substring(0, raw.length() - 2);
            }

            if (parentRef != null && !parentRef.isEmpty()) {
                final String ref = parentRef;
                Optional<Lookup> byCode = lookupRepository.findByCode(ref);
                if (byCode.isPresent()) {
                    return byCode.get();
                }

                try {
                    Long id = Long.parseLong(ref);
                    Optional<Lookup> byId = lookupRepository.findById(id);
                    if (byId.isPresent()) {
                        return byId.get();
                    }
                } catch (NumberFormatException ignored) {}

                throw new LookupNotFoundException("Parent lookup not found for reference: " + ref);
            }
        }

        throw new LookupBadRequestException("Parent lookup identifier ('lookup_code' or 'code') is required");
    }

    /**
     * Updates an existing sub lookup (name, description, status, displayOrder).
     */
    @Transactional(transactionManager = "bankTransactionManager")
    public SubLookupResponse updateSubLookup(Long subLookupId, SubLookupUpdateRequest request) {
        if (request == null) {
            throw new LookupBadRequestException("Update request cannot be null");
        }

        SubLookup subLookup = subLookupRepository.findById(subLookupId)
                .orElseThrow(() -> new LookupNotFoundException("Sub lookup not found with ID: " + subLookupId));

        String effectiveDescription = request.getEffectiveDescription();
        if (effectiveDescription != null && !effectiveDescription.trim().isEmpty()) {
            subLookup.setSubTypeDescription(effectiveDescription.trim());
        }

        if (request.getDisplayOrder() != null) {
            subLookup.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            boolean newActive = parseStatus(request.getStatus(), subLookup.getIsActive());
            if (!newActive && Boolean.TRUE.equals(subLookup.getIsFixed())) {
                throw new LookupBadRequestException(String.format(
                        "Fixed sub lookup '%s' under lookup '%s' cannot be deactivated",
                        subLookup.getSubTypeCode(), subLookup.getLookupTypeCode()));
            }
            subLookup.setIsActive(newActive);
        }

        subLookup.setModifiedBy(getCurrentUserId());
        SubLookup saved = subLookupRepository.save(subLookup);
        cacheManager.evictLookup(subLookup.getLookupTypeCode());

        logger.info("Updated sub lookup '{}:{}' (id={}) by user {}",
                subLookup.getLookupTypeCode(), subLookup.getSubTypeCode(), saved.getId(), saved.getModifiedBy());
        return toSubLookupResponse(saved);
    }

    /**
     * Toggles status between Active and Inactive for a sub lookup.
     */
    @Transactional(transactionManager = "bankTransactionManager")
    public SubLookupResponse toggleSubLookupStatus(Long subLookupId, StatusUpdateRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new LookupBadRequestException("Status update request cannot be null");
        }

        SubLookup subLookup = subLookupRepository.findById(subLookupId)
                .orElseThrow(() -> new LookupNotFoundException("Sub lookup not found with ID: " + subLookupId));

        boolean newActive = request.isActive();
        if (!newActive && Boolean.TRUE.equals(subLookup.getIsFixed())) {
            throw new LookupBadRequestException(String.format(
                    "Fixed sub lookup '%s' under lookup '%s' cannot be deactivated",
                    subLookup.getSubTypeCode(), subLookup.getLookupTypeCode()));
        }

        subLookup.setIsActive(newActive);
        subLookup.setModifiedBy(getCurrentUserId());
        SubLookup saved = subLookupRepository.save(subLookup);
        cacheManager.evictLookup(subLookup.getLookupTypeCode());

        logger.info("Toggled sub lookup '{}:{}' to {} by user {}",
                subLookup.getLookupTypeCode(), subLookup.getSubTypeCode(), newActive ? "Active" : "Inactive", saved.getModifiedBy());
        return toSubLookupResponse(saved);
    }

    // =====================================================================
    // HELPER & MAPPING METHODS
    // =====================================================================

    public LookupResponse toLookupResponseWithSubLookups(Lookup entity) {
        if (entity == null) return null;
        List<SubLookup> subs = entity.getSubLookups() != null ? entity.getSubLookups() : Collections.emptyList();
        return toLookupResponse(entity, subs);
    }

    public LookupResponse toLookupResponse(Lookup entity, List<SubLookup> subLookups) {
        if (entity == null) return null;

        List<SubLookupResponse> subDtos = subLookups != null
                ? subLookups.stream().map(this::toSubLookupResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return LookupResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getDescription())
                .description(entity.getDescription())
                .status(Boolean.TRUE.equals(entity.getIsActive()) ? "Active" : "Inactive")
                .isFixed(Boolean.TRUE.equals(entity.getIsFixed()))
                .subLookupCount(subDtos.size())
                .createdAt(entity.getCreatedAt())
                .subLookups(subDtos)
                .build();
    }

    public SubLookupResponse toSubLookupResponse(SubLookup entity) {
        if (entity == null) return null;

        return SubLookupResponse.builder()
                .id(entity.getId())
                .lookupTypeCode(entity.getLookupTypeCode())
                .subLookupCode(entity.getSubTypeCode())
                .code(entity.getSubTypeCode())
                .name(entity.getSubTypeDescription())
                .description(entity.getSubTypeDescription())
                .status(Boolean.TRUE.equals(entity.getIsActive()) ? "Active" : "Inactive")
                .displayOrder(entity.getDisplayOrder())
                .isFixed(Boolean.TRUE.equals(entity.getIsFixed()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private void validateDigitsOnly(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new LookupBadRequestException(fieldName + " is required");
        }
        if (!DIGITS_ONLY.matcher(value.trim()).matches()) {
            throw new LookupBadRequestException(fieldName + " must contain digits only: '" + value + "'");
        }
    }

    private boolean parseStatus(String statusStr, boolean defaultVal) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return defaultVal;
        }
        if ("ACTIVE".equalsIgnoreCase(statusStr.trim())) {
            return true;
        }
        if ("INACTIVE".equalsIgnoreCase(statusStr.trim())) {
            return false;
        }
        throw new LookupBadRequestException("Invalid status: '" + statusStr + "'. Expected 'Active' or 'Inactive'.");
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "ALL";
        }
        String s = status.trim().toUpperCase();
        if ("ACTIVE".equals(s) || "INACTIVE".equals(s)) {
            return s;
        }
        return "ALL";
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Number) {
                return ((Number) principal).longValue();
            }
            try {
                return Long.parseLong(auth.getName());
            } catch (NumberFormatException ignored) {}
        }
        return 1L; // Fallback to initial administrator ID
    }
}
