package com.los.bank.rule.service;

import com.los.bank.rule.dto.RuleRequestDto;
import com.los.bank.rule.dto.RuleResponseDto;
import com.los.bank.rule.entity.Rule;
import com.los.bank.rule.exception.RuleNotFoundException;
import com.los.bank.rule.exception.RuleValidationException;
import com.los.bank.rule.repository.RuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation managing business rules in the Rule Engine module.
 * Operates transactionally under 'bankTransactionManager' for tenant data isolation.
 */
@Service
public class RuleServiceImpl implements RuleService {

    private static final Logger logger = LoggerFactory.getLogger(RuleServiceImpl.class);

    private static final Set<String> SUPPORTED_STATUSES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("ACTIVE", "INACTIVE"))
    );

    private static final Set<String> RANGE_OPERATORS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("BETWEEN", "NOT_BETWEEN"))
    );

    private static final Set<String> SINGLE_VALUE_OPERATORS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "EQUALS", "EQUAL_TO", "==", "=",
                    "NOT_EQUALS", "NOT_EQUAL_TO", "!=", "<>",
                    "GREATER_THAN", ">",
                    "GREATER_THAN_OR_EQUAL", "GREATER_THAN_OR_EQUAL_TO", ">=",
                    "LESS_THAN", "<",
                    "LESS_THAN_OR_EQUAL", "LESS_THAN_OR_EQUAL_TO", "<=",
                    "IN", "NOT_IN"
            ))
    );

    private static final Set<String> SUPPORTED_ACTIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("ALLOW", "APPROVE", "REJECT", "DECLINE", "REFER", "REVIEW", "MANUAL_REVIEW"))
    );

    private final RuleRepository ruleRepository;

    public RuleServiceImpl(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager")
    public RuleResponseDto createRule(RuleRequestDto requestDto) {
        logger.info("Initiating rule creation for param: '{}', operator: '{}'",
                requestDto != null ? requestDto.getParam() : null,
                requestDto != null ? requestDto.getComparisonOperator() : null);

        // 1. Comprehensive input validation
        validateRuleRequest(requestDto);

        String normalizedStatus = normalizeStatus(requestDto.getStatus());
        String trimmedParam = requestDto.getParam().trim();
        String normalizedOperator = normalizeOperator(requestDto.getComparisonOperator());
        String normalizedAction = normalizeAction(requestDto.getActionOnMatch());
        int offset = requestDto.getOffset() != null ? requestDto.getOffset() : 0;

        String fromVal = requestDto.getFrom() != null && !requestDto.getFrom().trim().isEmpty()
                ? requestDto.getFrom().trim() : null;
        String toVal = requestDto.getTo() != null && !requestDto.getTo().trim().isEmpty()
                ? requestDto.getTo().trim() : null;
        String val = requestDto.getValue() != null && !requestDto.getValue().trim().isEmpty()
                ? requestDto.getValue().trim() : null;

        // 2. Duplicate rule validation
        boolean exists = ruleRepository.existsByParamIgnoreCaseAndComparisonOperatorIgnoreCaseAndFromValueAndToValueAndValAndStatusIgnoreCase(
                trimmedParam, normalizedOperator, fromVal, toVal, val, normalizedStatus);
        if (exists) {
            logger.warn("Rule creation rejected: duplicate rule configuration exists for param '{}'", trimmedParam);
            throw new RuleValidationException("A rule with the same parameter, operator, boundaries, and status already exists.");
        }

        // 3. Map to Entity
        Rule rule = Rule.builder()
                .status(normalizedStatus)
                .param(trimmedParam)
                .comparisonOperator(normalizedOperator)
                .fromValue(fromVal)
                .toValue(toVal)
                .val(val)
                .actionOnMatch(normalizedAction)
                .executionOffset(offset)
                .build();

        Rule saved = ruleRepository.save(rule);
        logger.info("Rule created successfully with ID: {}", saved.getId());

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "bankTransactionManager")
    public Map<String, Object> getAllRules(int page, int size, String status, String param) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "executionOffset").and(Sort.by(Sort.Direction.DESC, "id")));

        String filterStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        String filterParam = (param != null && !param.trim().isEmpty()) ? param.trim() : null;

        Page<Rule> rulePage = ruleRepository.findWithFilters(filterStatus, filterParam, pageable);

        List<RuleResponseDto> dtoList = rulePage.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("totalElements", rulePage.getTotalElements());
        result.put("totalPages", rulePage.getTotalPages());
        result.put("currentPage", rulePage.getNumber());
        result.put("pageSize", rulePage.getSize());
        result.put("data", dtoList);

        return result;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "bankTransactionManager")
    public RuleResponseDto getRuleById(Long id) {
        if (id == null) {
            throw new RuleValidationException("Rule ID cannot be null.");
        }
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuleNotFoundException("Rule not found with id: " + id));
        return mapToResponseDto(rule);
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager")
    public RuleResponseDto updateRule(Long id, RuleRequestDto requestDto) {
        if (id == null) {
            throw new RuleValidationException("Rule ID cannot be null.");
        }
        logger.info("Initiating rule update for ID: {}", id);

        Rule existing = ruleRepository.findById(id)
                .orElseThrow(() -> new RuleNotFoundException("Rule not found with id: " + id));

        validateRuleRequest(requestDto);

        String normalizedStatus = normalizeStatus(requestDto.getStatus());
        String trimmedParam = requestDto.getParam().trim();
        String normalizedOperator = normalizeOperator(requestDto.getComparisonOperator());
        String normalizedAction = normalizeAction(requestDto.getActionOnMatch());
        int offset = requestDto.getOffset() != null ? requestDto.getOffset() : 0;

        String fromVal = requestDto.getFrom() != null && !requestDto.getFrom().trim().isEmpty()
                ? requestDto.getFrom().trim() : null;
        String toVal = requestDto.getTo() != null && !requestDto.getTo().trim().isEmpty()
                ? requestDto.getTo().trim() : null;
        String val = requestDto.getValue() != null && !requestDto.getValue().trim().isEmpty()
                ? requestDto.getValue().trim() : null;

        existing.setStatus(normalizedStatus);
        existing.setParam(trimmedParam);
        existing.setComparisonOperator(normalizedOperator);
        existing.setFromValue(fromVal);
        existing.setToValue(toVal);
        existing.setVal(val);
        existing.setActionOnMatch(normalizedAction);
        existing.setExecutionOffset(offset);

        Rule updated = ruleRepository.save(existing);
        logger.info("Rule updated successfully for ID: {}", updated.getId());

        return mapToResponseDto(updated);
    }

    // =========================================================================
    // VALIDATION HELPERS
    // =========================================================================

    private void validateRuleRequest(RuleRequestDto request) {
        if (request == null) {
            throw new RuleValidationException("Request payload cannot be null.");
        }

        // 1. Validate status
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            String upperStatus = request.getStatus().trim().toUpperCase(Locale.ROOT);
            if (!SUPPORTED_STATUSES.contains(upperStatus)) {
                throw new RuleValidationException("Invalid status: '" + request.getStatus() +
                        "'. Permitted values are: active, inactive.");
            }
        }

        // 2. Validate param
        if (request.getParam() == null || request.getParam().trim().isEmpty()) {
            throw new RuleValidationException("Parameter ('param') is required.");
        }

        // 3. Validate comparison operator
        if (request.getComparisonOperator() == null || request.getComparisonOperator().trim().isEmpty()) {
            throw new RuleValidationException("Comparison operator ('comparisonOperator') is required.");
        }
        String opUpper = request.getComparisonOperator().trim().toUpperCase(Locale.ROOT);
        boolean isRange = RANGE_OPERATORS.contains(opUpper);
        boolean isSingle = SINGLE_VALUE_OPERATORS.contains(opUpper);

        if (!isRange && !isSingle) {
            throw new RuleValidationException("Unsupported comparison operator: '" + request.getComparisonOperator() +
                    "'. Supported operators include: between, equals, not_equals, greater_than, greater_than_or_equal, less_than, less_than_or_equal.");
        }

        // 4. Operator-specific validations
        String trimmedParam = request.getParam().trim();
        boolean isCibil = isCibilParameter(trimmedParam);

        if (isRange) {
            // Both 'from' and 'to' must be supplied
            if (request.getFrom() == null || request.getFrom().trim().isEmpty()) {
                throw new RuleValidationException("Lower boundary ('from') is required for range operator '" + request.getComparisonOperator() + "'.");
            }
            if (request.getTo() == null || request.getTo().trim().isEmpty()) {
                throw new RuleValidationException("Upper boundary ('to') is required for range operator '" + request.getComparisonOperator() + "'.");
            }

            BigDecimal fromNum = parseNumeric(request.getFrom(), "from", isCibil);
            BigDecimal toNum = parseNumeric(request.getTo(), "to", isCibil);

            if (fromNum != null && toNum != null) {
                if (fromNum.compareTo(toNum) > 0) {
                    throw new RuleValidationException("Lower boundary ('from') cannot be greater than upper boundary ('to').");
                }
            }
        } else {
            // Single-value operators require 'value'
            if (request.getValue() == null || request.getValue().trim().isEmpty()) {
                throw new RuleValidationException("Comparison value ('value') is required for operator '" + request.getComparisonOperator() + "'.");
            }
            if (isCibil) {
                parseNumeric(request.getValue(), "value", true);
            }
        }

        // 5. Validate action on match
        if (request.getActionOnMatch() == null || request.getActionOnMatch().trim().isEmpty()) {
            throw new RuleValidationException("Action on match ('actionOnMatch') is required.");
        }
        String actionUpper = request.getActionOnMatch().trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        if (!SUPPORTED_ACTIONS.contains(actionUpper)) {
            throw new RuleValidationException("Unsupported action on match: '" + request.getActionOnMatch() +
                    "'. Permitted actions are: Allow, Reject, Refer, Review.");
        }

        // 6. Validate offset
        if (request.getOffset() != null && request.getOffset() < 0) {
            throw new RuleValidationException("Execution offset ('offset') must be greater than or equal to 0.");
        }
    }

    private boolean isCibilParameter(String param) {
        if (param == null) return false;
        String lower = param.toLowerCase(Locale.ROOT);
        return lower.contains("cibil");
    }

    private BigDecimal parseNumeric(String valueStr, String fieldName, boolean mandatoryNumeric) {
        if (valueStr == null || valueStr.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(valueStr.trim());
        } catch (NumberFormatException e) {
            if (mandatoryNumeric) {
                throw new RuleValidationException("Field '" + fieldName + "' must be a valid numeric value for parameter condition: " + valueStr);
            }
            return null;
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "active";
        }
        return status.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOperator(String operator) {
        if (operator == null) return null;
        String trimmed = operator.trim();
        // Canonicalize common representations
        if ("between".equalsIgnoreCase(trimmed)) return "between";
        if ("==".equals(trimmed) || "=".equals(trimmed) || "equal_to".equalsIgnoreCase(trimmed) || "equals".equalsIgnoreCase(trimmed)) return "equals";
        if ("!=".equals(trimmed) || "<>".equals(trimmed) || "not_equal_to".equalsIgnoreCase(trimmed) || "not_equals".equalsIgnoreCase(trimmed)) return "not_equals";
        if (">".equals(trimmed) || "greater_than".equalsIgnoreCase(trimmed)) return "greater_than";
        if (">=".equals(trimmed) || "greater_than_or_equal".equalsIgnoreCase(trimmed) || "greater_than_or_equal_to".equalsIgnoreCase(trimmed)) return "greater_than_or_equal";
        if ("<".equals(trimmed) || "less_than".equalsIgnoreCase(trimmed)) return "less_than";
        if ("<=".equals(trimmed) || "less_than_or_equal".equalsIgnoreCase(trimmed) || "less_than_or_equal_to".equalsIgnoreCase(trimmed)) return "less_than_or_equal";
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String normalizeAction(String action) {
        if (action == null) return null;
        String trimmed = action.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT).replace(" ", "_");
        if ("ALLOW".equals(upper) || "APPROVE".equals(upper)) return "Allow";
        if ("REJECT".equals(upper) || "DECLINE".equals(upper)) return "Reject";
        if ("REFER".equals(upper)) return "Refer";
        if ("REVIEW".equals(upper) || "MANUAL_REVIEW".equals(upper)) return "Review";
        return trimmed;
    }

    private RuleResponseDto mapToResponseDto(Rule rule) {
        if (rule == null) return null;
        return RuleResponseDto.builder()
                .id(rule.getId())
                .status(rule.getStatus())
                .param(rule.getParam())
                .comparisonOperator(rule.getComparisonOperator())
                .from(rule.getFromValue())
                .to(rule.getToValue())
                .value(rule.getVal())
                .actionOnMatch(rule.getActionOnMatch())
                .offset(rule.getExecutionOffset())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
