package com.los.bank.lookup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubLookupCreateRequest {

    /**
     * Dedicated sub lookup code (e.g. "1000801", where 10008 is parent lookup code and 01 is sub code).
     */
    @JsonProperty("sub_lookup_code")
    @JsonAlias({"subLookupCode", "sublookup_code", "sub_type_code", "subTypeCode", "sub_code", "subCode"})
    @Pattern(regexp = "^\\d+$", message = "Sub lookup code must contain digits only")
    @Size(max = 50, message = "Sub lookup code cannot exceed 50 characters")
    private String subLookupCode;

    /**
     * Optional / generic code field.
     * Can receive the parent lookup reference (e.g. "4") or the sublookup code ("1000801").
     */
    @JsonProperty("code")
    @Pattern(regexp = "^\\d+$", message = "Code must contain digits only")
    @Size(max = 50, message = "Code cannot exceed 50 characters")
    private String code;

    /**
     * Optional parent lookup code if explicitly supplied in the request body (e.g. "10008").
     */
    @JsonProperty("lookup_code")
    @JsonAlias({"lookupCode", "lookup_type_code", "lookupTypeCode"})
    private String lookupCode;

    @Size(max = 255, message = "Sub lookup name cannot exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Pattern(regexp = "^(?i)(ACTIVE|INACTIVE)$", message = "Status must be 'Active' or 'Inactive'")
    @Builder.Default
    private String status = "Active";

    @JsonProperty("display_order")
    @JsonAlias({"displayOrder", "order"})
    private Integer displayOrder;

    @JsonProperty("is_fixed")
    @JsonAlias({"isFixed", "fixed"})
    @Builder.Default
    private Boolean isFixed = false;

    /**
     * Resolves the effective sub lookup code based on parent lookup code context.
     * Prefers explicit sub_lookup_code over code.
     * If the sub code is provided as a short relative suffix (e.g. "01" or "1"),
     * it ensures the standard formatted pattern <parentCode><suffix> (e.g. "1000801").
     */
    public String getEffectiveSubLookupCode(String parentLookupCode) {
        String rawCode = null;
        if (subLookupCode != null && !subLookupCode.trim().isEmpty()) {
            rawCode = subLookupCode.trim();
        } else if (code != null && !code.trim().isEmpty()) {
            String trimmedCode = code.trim();
            if (parentLookupCode != null && !parentLookupCode.trim().isEmpty()) {
                // If code matches the parent lookup code prefix, it's the full sublookup code (e.g. "1000801")
                if (trimmedCode.startsWith(parentLookupCode.trim())) {
                    rawCode = trimmedCode;
                }
            } else {
                rawCode = trimmedCode;
            }
        }

        if (rawCode == null || rawCode.isEmpty()) {
            return null;
        }

        if (parentLookupCode != null && !parentLookupCode.trim().isEmpty()) {
            String normParent = parentLookupCode.trim();
            // If already formatted with parent prefix (e.g. "1000801" with parent "10008"), keep as is
            if (rawCode.startsWith(normParent)) {
                return rawCode;
            }
            // If user supplied relative suffix (e.g. "01" or "1"), auto-format to <parentCode><suffix>
            if (rawCode.length() <= 3) {
                String suffix = rawCode.length() == 1 ? "0" + rawCode : rawCode;
                return normParent + suffix;
            }
        }

        return rawCode;
    }

    public String getEffectiveDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description.trim();
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        return null;
    }
}
