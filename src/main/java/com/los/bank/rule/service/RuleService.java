package com.los.bank.rule.service;

import com.los.bank.rule.dto.RuleOptionsDto;
import com.los.bank.rule.dto.RuleRequestDto;
import com.los.bank.rule.dto.RuleResponseDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface RuleService {

    /** Dropdown values for the Rule Engine screen, from Lookup Master. */
    RuleOptionsDto getOptions();

    RuleResponseDto createRule(RuleRequestDto requestDto, Long staffUserId);

    Map<String, Object> getAllRules(int page, int size, String status, String param, String decision);

    RuleResponseDto getRuleById(Long id);

    RuleResponseDto updateRule(Long id, RuleRequestDto requestDto, Long staffUserId);

    RuleResponseDto changeStatus(Long id, String status, Long staffUserId);

    void deleteRule(Long id);

    /** Active rules whose effective date has arrived, with their lookup values resolved. */
    List<ResolvedRule> getRulesInForce(LocalDate date);
}
