package com.los.bank.interestrate.service;

import com.los.bank.interestrate.dto.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InterestRateMasterService {

    InterestRateMasterResponse create(InterestRateMasterRequest request, Long staffUserId);

    InterestRateMasterResponse update(Long id, InterestRateMasterRequest request, Long staffUserId);

    void delete(Long id, Long staffUserId);

    InterestRateMasterResponse approve(Long id, Long staffUserId);

    InterestRateMasterResponse reject(Long id, String reason, Long staffUserId);

    InterestRateMasterResponse getById(Long id);

    InterestRatePageResponse<InterestRateMasterResponse> list(String productCode, String status, int page, int size);

    /**
     * Rate a new loan of this product, amount and tenure gets on {@code asOfDate} (today when null).
     */
    ApplicableRateResponse findApplicableRate(String productCode, BigDecimal amount, Integer tenureMonths, LocalDate asOfDate);
}
