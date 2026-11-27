package com.los.bank.rule.service;

import com.los.bank.rule.dto.RuleRequestDto;
import com.los.bank.rule.dto.RuleResponseDto;

import java.util.Map;

public interface RuleService {

    RuleResponseDto createRule(RuleRequestDto requestDto);

    Map<String, Object> getAllRules(int page, int size, String status, String param);

    RuleResponseDto getRuleById(Long id);

    RuleResponseDto updateRule(Long id, RuleRequestDto requestDto);
}
