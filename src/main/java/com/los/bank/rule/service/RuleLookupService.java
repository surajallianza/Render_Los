package com.los.bank.rule.service;

import com.los.bank.lookup.entity.Lookup;
import com.los.bank.lookup.entity.SubLookup;
import com.los.bank.lookup.repository.LookupRepository;
import com.los.bank.lookup.repository.SubLookupRepository;
import com.los.bank.rule.config.RuleLookupProperties;
import com.los.bank.rule.entity.*;
import com.los.bank.rule.exception.RuleValidationException;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Reads the Rule Engine dropdown values from Lookup Master and works out what each one means.
 */
@Service
public class RuleLookupService {

    public record LookupRef(Long id, String code, String name) {
        static LookupRef of(SubLookup sub) {
            return new LookupRef(sub.getId(), sub.getSubTypeCode(), sub.getSubTypeDescription());
        }
    }

    /**
     * A "Rule Parameter" value. {@code metric} is set for FOIR / DTI / LTV / CIBIL (calculated by the
     * eligibility check); a choice parameter has {@code options} from the lookup with the same name.
     */
    public record ParamInfo(LookupRef ref, RuleMetric metric, String unit, List<LookupRef> options) {
        public boolean isChoice() {
            return options != null && !options.isEmpty();
        }
    }

    public record OperatorInfo(LookupRef ref, RuleOperator operator) {
    }

    public record DecisionInfo(LookupRef ref, RuleDecision decision) {
    }

    private final LookupRepository lookupRepository;
    private final SubLookupRepository subLookupRepository;
    private final RuleLookupProperties properties;

    public RuleLookupService(LookupRepository lookupRepository, SubLookupRepository subLookupRepository,
                             RuleLookupProperties properties) {
        this.lookupRepository = lookupRepository;
        this.subLookupRepository = subLookupRepository;
        this.properties = properties;
    }

    // ---------------------------------------------------------------------
    // Dropdown lists
    // ---------------------------------------------------------------------

    public List<ParamInfo> parameters() {
        return activeSubs(categoryCode(properties.getParameter())).stream().map(this::toParamInfo).toList();
    }

    public List<OperatorInfo> operators() {
        return activeSubs(categoryCode(properties.getOperator())).stream()
                .map(s -> new OperatorInfo(LookupRef.of(s), RuleOperator.fromText(s.getSubTypeDescription()).orElse(null)))
                .toList();
    }

    public List<DecisionInfo> decisions() {
        return activeSubs(categoryCode(properties.getDecision())).stream()
                .map(s -> new DecisionInfo(LookupRef.of(s), RuleDecision.fromText(s.getSubTypeDescription()).orElse(null)))
                .toList();
    }

    // ---------------------------------------------------------------------
    // Resolving request values (code, name or id) for create / update
    // ---------------------------------------------------------------------

    public Optional<ParamInfo> findParameter(String codeOrName) {
        return findActiveSub(properties.getParameter(), codeOrName).map(this::toParamInfo);
    }

    public Optional<OperatorInfo> findOperator(String codeOrName) {
        Optional<SubLookup> sub = findActiveSub(properties.getOperator(), codeOrName);
        if (sub.isEmpty()) {
            // The screen may send its short codes (lt / gt / between / eq): match by meaning
            Optional<RuleOperator> wanted = RuleOperator.fromText(codeOrName);
            if (wanted.isPresent()) {
                sub = activeSubs(categoryCode(properties.getOperator())).stream()
                        .filter(s -> RuleOperator.fromText(s.getSubTypeDescription()).orElse(null) == wanted.get())
                        .findFirst();
            }
        }
        return sub.map(s -> new OperatorInfo(LookupRef.of(s), RuleOperator.fromText(s.getSubTypeDescription()).orElse(null)));
    }

    public Optional<DecisionInfo> findDecision(String codeOrName) {
        return findActiveSub(properties.getDecision(), codeOrName)
                .map(s -> new DecisionInfo(LookupRef.of(s), RuleDecision.fromText(s.getSubTypeDescription()).orElse(null)));
    }

    public Optional<LookupRef> findOption(ParamInfo param, String codeOrName) {
        if (!param.isChoice() || codeOrName == null) {
            return Optional.empty();
        }
        String key = codeOrName.trim();
        return param.options().stream()
                .filter(o -> o.code().equalsIgnoreCase(key) || o.name().equalsIgnoreCase(key) || String.valueOf(o.id()).equals(key))
                .findFirst();
    }

    // ---------------------------------------------------------------------
    // Resolving stored rules (by id, active or not)
    // ---------------------------------------------------------------------

    /**
     * Loads all lookup values a list of rules refers to, in one query.
     */
    public Map<Long, SubLookup> loadByIds(Collection<Long> ids) {
        Map<Long, SubLookup> map = new HashMap<>();
        Set<Long> unique = new HashSet<>(ids);
        unique.remove(null);
        if (!unique.isEmpty()) {
            subLookupRepository.findAllById(unique).forEach(s -> map.put(s.getId(), s));
        }
        return map;
    }

    public ParamInfo toParamInfo(SubLookup sub) {
        RuleMetric metric = RuleMetric.fromLookupName(sub.getSubTypeDescription()).orElse(null);
        List<LookupRef> options = metric == null ? choiceOptions(sub) : List.of();
        String unit = metric != null ? metric.getUnit() : "";
        return new ParamInfo(LookupRef.of(sub), metric, unit, options);
    }

    /**
     * A parameter is a choice when a lookup with the same name exists (e.g. "Firm Registration"
     * with Registered / Not Registered).
     */
    private List<LookupRef> choiceOptions(SubLookup param) {
        String parameterCategory = categoryCodeOrNull(properties.getParameter());
        for (Lookup lookup : lookupRepository.findByNormalizedName(param.getSubTypeDescription())) {
            if (!lookup.getCode().equals(parameterCategory) && Boolean.TRUE.equals(lookup.getIsActive())) {
                List<LookupRef> options = activeSubs(lookup.getCode()).stream().map(LookupRef::of).toList();
                if (!options.isEmpty()) {
                    return options;
                }
            }
        }
        return List.of();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Optional<SubLookup> findActiveSub(String category, String codeOrName) {
        if (codeOrName == null || codeOrName.isBlank()) {
            return Optional.empty();
        }
        String key = codeOrName.trim();
        String normalizedKey = normalize(key);
        return activeSubs(categoryCode(category)).stream()
                .filter(s -> s.getSubTypeCode().equalsIgnoreCase(key)
                        || String.valueOf(s.getId()).equals(key)
                        || normalize(s.getSubTypeDescription()).equals(normalizedKey))
                .findFirst();
    }

    private List<SubLookup> activeSubs(String categoryCode) {
        return subLookupRepository.findByLookupTypeCodeAndIsActiveTrueOrderByDisplayOrderAscSubTypeCodeAsc(categoryCode);
    }

    /**
     * Lookup code of a category given by name or code. Throws a clear error when it is missing.
     */
    public String categoryCode(String nameOrCode) {
        String code = categoryCodeOrNull(nameOrCode);
        if (code == null) {
            throw new RuleValidationException("Lookup '" + nameOrCode + "' is not configured. Create it in Lookup Master "
                    + "with its values first.", Map.of("lookup", nameOrCode));
        }
        return code;
    }

    private String categoryCodeOrNull(String nameOrCode) {
        if (nameOrCode == null || nameOrCode.isBlank()) {
            return null;
        }
        return lookupRepository.findByNormalizedName(nameOrCode.trim()).stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                .map(Lookup::getCode)
                .findFirst()
                .orElse(null);
    }

    static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }
}
