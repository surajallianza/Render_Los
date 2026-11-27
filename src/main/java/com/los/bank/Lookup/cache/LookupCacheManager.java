package com.los.bank.lookup.cache;

import com.los.bank.lookup.dto.LookupDropdownItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe high-performance in-memory cache for Lookup resolution and dropdowns.
 * Automatically evicted whenever lookups or sub lookups are created, updated, or toggled.
 */
@Component
public class LookupCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(LookupCacheManager.class);

    // Key: lookupCode + "_" + activeOnly -> List<LookupDropdownItem>
    private final Map<String, List<LookupDropdownItem>> dropdownCache = new ConcurrentHashMap<>();

    // Key: lookupCode + ":" + subCode -> m002.id
    private final Map<String, Long> resolvedIdCache = new ConcurrentHashMap<>();

    // Key: lookupCode + ":" + subCode -> subTypeDescription
    private final Map<String, String> resolvedNameCache = new ConcurrentHashMap<>();

    // Key: lookupCode + ":" + subCode -> Boolean
    private final Map<String, Boolean> validityCache = new ConcurrentHashMap<>();

    // Key: normalizedCategoryName -> parentCode
    private final Map<String, String> parentCodeCache = new ConcurrentHashMap<>();

    public String getParentCode(String lookupName) {
        if (lookupName == null) return null;
        return parentCodeCache.get(lookupName.trim().toLowerCase());
    }

    public void putParentCode(String lookupName, String parentCode) {
        if (lookupName != null && parentCode != null) {
            parentCodeCache.put(lookupName.trim().toLowerCase(), parentCode);
        }
    }

    public List<LookupDropdownItem> getDropdown(String lookupCode, boolean activeOnly) {
        return dropdownCache.get(dropdownKey(lookupCode, activeOnly));
    }

    public void putDropdown(String lookupCode, boolean activeOnly, List<LookupDropdownItem> items) {
        if (lookupCode != null && items != null) {
            dropdownCache.put(dropdownKey(lookupCode, activeOnly), items);
        }
    }

    public Long getResolvedId(String lookupCode, String subCode) {
        return resolvedIdCache.get(cacheKey(lookupCode, subCode));
    }

    public void putResolvedId(String lookupCode, String subCode, Long id) {
        if (lookupCode != null && subCode != null && id != null) {
            resolvedIdCache.put(cacheKey(lookupCode, subCode), id);
        }
    }

    public String getResolvedName(String lookupCode, String subCode) {
        return resolvedNameCache.get(cacheKey(lookupCode, subCode));
    }

    public void putResolvedName(String lookupCode, String subCode, String name) {
        if (lookupCode != null && subCode != null && name != null) {
            resolvedNameCache.put(cacheKey(lookupCode, subCode), name);
        }
    }

    public Boolean getValidity(String lookupCode, String subCode) {
        return validityCache.get(cacheKey(lookupCode, subCode));
    }

    public void putValidity(String lookupCode, String subCode, boolean isValid) {
        if (lookupCode != null && subCode != null) {
            validityCache.put(cacheKey(lookupCode, subCode), isValid);
        }
    }

    /**
     * Evicts all cache entries for a specific lookup code.
     */
    public void evictLookup(String lookupCode) {
        if (lookupCode == null) return;
        dropdownCache.remove(dropdownKey(lookupCode, true));
        dropdownCache.remove(dropdownKey(lookupCode, false));

        String prefix = lookupCode.trim().toUpperCase() + ":";
        resolvedIdCache.keySet().removeIf(k -> k.startsWith(prefix));
        resolvedNameCache.keySet().removeIf(k -> k.startsWith(prefix));
        validityCache.keySet().removeIf(k -> k.startsWith(prefix));

        logger.debug("Evicted lookup cache for code '{}'", lookupCode);
    }

    /**
     * Clears all lookup caches entirely.
     */
    public void evictAll() {
        dropdownCache.clear();
        resolvedIdCache.clear();
        resolvedNameCache.clear();
        validityCache.clear();
        parentCodeCache.clear();
        logger.info("Cleared all lookup and sub-lookup caches");
    }

    private String dropdownKey(String lookupCode, boolean activeOnly) {
        return (lookupCode != null ? lookupCode.trim().toUpperCase() : "") + "_" + activeOnly;
    }

    private String cacheKey(String lookupCode, String subCode) {
        return (lookupCode != null ? lookupCode.trim().toUpperCase() : "") + ":" +
               (subCode != null ? subCode.trim() : "");
    }
}
