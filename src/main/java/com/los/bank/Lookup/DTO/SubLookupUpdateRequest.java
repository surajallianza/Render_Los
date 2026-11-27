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
public class SubLookupUpdateRequest {

    @Size(max = 255, message = "Sub lookup name cannot exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Pattern(regexp = "^(?i)(ACTIVE|INACTIVE)$", message = "Status must be 'Active' or 'Inactive'")
    private String status;

    @JsonProperty("display_order")
    @JsonAlias({"displayOrder", "order"})
    private Integer displayOrder;

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
