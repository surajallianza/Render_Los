package com.los.bank.lookup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
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
public class LookupCreateRequest {

    @NotBlank(message = "Lookup code is required")
    @Pattern(regexp = "^\\d+$", message = "Lookup code must contain digits only")
    @Size(max = 50, message = "Lookup code cannot exceed 50 characters")
    private String code;

    @Size(max = 255, message = "Lookup name cannot exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Pattern(regexp = "^(?i)(ACTIVE|INACTIVE)$", message = "Status must be 'Active' or 'Inactive'")
    @Builder.Default
    private String status = "Active";

    @JsonProperty("is_fixed")
    @JsonAlias({"isFixed", "fixed"})
    @Builder.Default
    private Boolean isFixed = false;

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
