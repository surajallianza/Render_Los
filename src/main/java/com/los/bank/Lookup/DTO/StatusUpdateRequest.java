package com.los.bank.lookup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(?i)(ACTIVE|INACTIVE)$", message = "Status must be 'Active' or 'Inactive'")
    private String status;

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
