package com.los.bank.loanEligibility.service;

import com.los.bank.loanEligibility.dto.EligibilityRequest;
import com.los.bank.loanEligibility.exception.EligibilityValidationException;
import com.los.bank.rule.entity.RuleMetric;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * Loan eligibility formulas, as on the Loan Eligibility screen:
 * <ul>
 *   <li>Net income = total income - total expenses</li>
 *   <li>FOIR = (existing monthly debt + proposed EMI) / net income x 100</li>
 *   <li>DTI  = (existing monthly debt + proposed EMI) / gross (total) income x 100</li>
 *   <li>LTV  = requested loan / security value x 100 (only when a security value is given)</li>
 *   <li>CIBIL score is used as entered</li>
 * </ul>
 */
@Component
public class EligibilityCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final MathContext MC = MathContext.DECIMAL64;

    public record Inputs(BigDecimal requestedAmount,
                         BigDecimal annualInterestRate,
                         int tenureMonths,
                         int cibilScore,
                         BigDecimal totalIncome,
                         BigDecimal totalExpenses,
                         BigDecimal netIncome,
                         BigDecimal existingMonthlyDebt,
                         BigDecimal securityValue,
                         List<String> warnings) {

        public boolean ltvApplicable() {
            return securityValue != null;
        }
    }

    public Inputs prepare(EligibilityRequest request) {
        if (request == null) {
            throw new EligibilityValidationException("Request body is required", Map.of("request", "Request body is required"));
        }
        Map<String, String> errors = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        BigDecimal amount = request.getRequestedAmount();
        if (amount == null || amount.signum() <= 0) {
            errors.put("requestedAmount", "Loan amount is required and must be greater than zero.");
        }
        BigDecimal rate = request.getAnnualInterestRate();
        if (rate == null || rate.signum() < 0 || rate.compareTo(HUNDRED) > 0) {
            errors.put("annualInterestRate", "Interest rate is required and must be between 0 and 100.");
        }
        Integer tenure = request.getTenureMonths();
        if (tenure == null || tenure < 1 || tenure > 480) {
            errors.put("tenureMonths", "Tenure is required and must be between 1 and 480 months.");
        }
        Integer cibil = request.getCibilScore();
        if (cibil == null) {
            errors.put("cibilScore", "CIBIL score is required.");
        } else if (cibil < 0 || cibil > 900) {
            errors.put("cibilScore", "CIBIL score must be between 0 and 900.");
        }

        BigDecimal debt = request.getExistingMonthlyDebt();
        if (debt == null && request.getCalculatedMetrics() != null) {
            debt = request.getCalculatedMetrics().getExistingMonthlyDebt();
        }
        if (debt == null) {
            debt = BigDecimal.ZERO;
        } else if (debt.signum() < 0) {
            errors.put("existingMonthlyDebt", "Existing monthly debt cannot be negative.");
        }

        BigDecimal security = request.getSecurityValue();
        if (security != null && security.signum() < 0) {
            errors.put("securityValue", "Security value cannot be negative.");
        }
        if (security != null && security.signum() == 0) {
            security = null;
        }

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        List<EligibilityRequest.Row> rows = request.getRows() == null ? List.of() : request.getRows();
        for (int i = 0; i < rows.size(); i++) {
            EligibilityRequest.Row row = rows.get(i);
            String type = row.getType() == null ? "" : row.getType().trim();
            if (row.getAmount() == null || row.getAmount().signum() < 0) {
                errors.put("rows[" + i + "].amount", "Amount is required and cannot be negative.");
                continue;
            }
            if ("income".equalsIgnoreCase(type)) {
                income = income.add(row.getAmount());
            } else if ("expense".equalsIgnoreCase(type)) {
                expenses = expenses.add(row.getAmount());
            } else {
                errors.put("rows[" + i + "].type", "Type must be Income or Expense.");
            }
        }
        if (income.signum() <= 0 && !errors.containsKey("rows")) {
            errors.put("income", "Add at least one income entry greater than zero.");
        }
        BigDecimal net = income.subtract(expenses);
        if (income.signum() > 0 && net.signum() <= 0) {
            errors.put("rows", "Expenses are equal to or higher than income, so FOIR cannot be calculated.");
        }

        if (!errors.isEmpty()) {
            throw new EligibilityValidationException("Please correct the highlighted fields.", errors);
        }
        return new Inputs(amount, rate, tenure, cibil, scale2(income), scale2(expenses), scale2(net), scale2(debt),
                security == null ? null : scale2(security), warnings);
    }

    /** Values of the calculated parameters for a loan of {@code amount}; LTV only with a security value. */
    public Map<RuleMetric, BigDecimal> metrics(Inputs in, BigDecimal amount) {
        Map<RuleMetric, BigDecimal> values = new EnumMap<>(RuleMetric.class);
        values.put(RuleMetric.FOIR, foir(in, amount));
        values.put(RuleMetric.DTI, dti(in, amount));
        if (in.ltvApplicable()) {
            values.put(RuleMetric.LTV, ltv(in, amount));
        }
        values.put(RuleMetric.CIBIL_SCORE, BigDecimal.valueOf(in.cibilScore()));
        return values;
    }

    /** (existing monthly debt + EMI of amount) / net income x 100. */
    public BigDecimal foir(Inputs in, BigDecimal amount) {
        return percent(obligations(in, amount), in.netIncome());
    }

    /** (existing monthly debt + EMI of amount) / total income x 100. */
    public BigDecimal dti(Inputs in, BigDecimal amount) {
        return percent(obligations(in, amount), in.totalIncome());
    }

    /** amount / security value x 100, null without a security value. */
    public BigDecimal ltv(Inputs in, BigDecimal amount) {
        return in.ltvApplicable() ? percent(amount, in.securityValue()) : null;
    }

    private BigDecimal obligations(Inputs in, BigDecimal amount) {
        return in.existingMonthlyDebt().add(emi(amount, in.annualInterestRate(), in.tenureMonths()));
    }

    /** Monthly EMI (reducing balance): P x r x (1+r)^n / ((1+r)^n - 1), r = annual rate / 1200. */
    public static BigDecimal emi(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (principal == null || principal.signum() <= 0 || tenureMonths <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (annualRate == null || annualRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal r = annualRate.divide(BigDecimal.valueOf(1200), MC);
        BigDecimal factor = BigDecimal.ONE.add(r).pow(tenureMonths, MC);
        return principal.multiply(r, MC).multiply(factor, MC)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
