package com.los.bank.rule.dto;

import lombok.*;

/**
 * A Lookup Master value: id (m002.id), code (sub lookup code) and name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleLookupValue {
    private Long id;
    private String code;
    private String name;
}
