package com.los.bank.rule.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create / update payload, matching the Rule Engine screen.
 *
 * <pre>
 * { "param": "1001401", "op": "1001503", "a": 50, "decision": "1001603",
 *   "offset": 0, "effectiveDate": "2026-12-01" }
 * </pre>
 * param / op / decision take the Lookup Master sub code (from GET /api/rules/options), the lookup
 * name ("FOIR", "Above", "Reject") or its id. For Between send a (from) and b (to). For a choice
 * parameter (e.g. Firm Registration) send the option's code or name in a, with op "Is".
 * offset = "Add to rate (%)", required for Override.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleRequestDto {

    /** "Check". */
    @JsonAlias({"check", "parameter", "paramCode", "param_code", "paramLookupId"})
    private String param;

    /** "When value is". */
    @JsonAlias({"operator", "comparisonOperator", "operatorCode", "whenValueIs", "operatorLookupId"})
    private String op;

    /** Value, or "From" for Between, or the option for a choice parameter. */
    @JsonAlias({"value", "val", "from", "result"})
    private String a;

    /** "To" for Between. */
    @JsonAlias({"to"})
    private String b;

    /** "Then". */
    @JsonAlias({"then", "actionOnMatch", "decisionCode", "decisionLookupId"})
    private String decision;

    /** "Add to rate (%)", for Override. */
    @JsonAlias({"rateOffset", "rate_offset", "addToRate"})
    private BigDecimal offset;

    private String message;

    /** ACTIVE (default) or INACTIVE. */
    private String status;

    /** The screen's active flag; used when status is not sent. */
    private Boolean active;

    @JsonAlias({"effective_date"})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;
}
