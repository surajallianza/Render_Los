package com.los.bank.lookup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reusable DTO for loan application and onboarding responses where a lookup field
 * returns both code and human-readable name:
 * e.g., { "loanType": { "code": "1", "name": "Gold Loan" } }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LookupValue {
    private String code;
    private String name;

    public static LookupValue of(String code, String name) {
        return new LookupValue(code, name);
    }
}
