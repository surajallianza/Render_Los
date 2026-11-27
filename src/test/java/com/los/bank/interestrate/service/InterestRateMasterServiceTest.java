package com.los.bank.interestrate.service;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import com.los.bank.interestrate.dto.*;
import com.los.bank.interestrate.entity.InterestRateMaster;
import com.los.bank.interestrate.entity.InterestRateStatus;
import com.los.bank.interestrate.entity.InterestRateTier;
import com.los.bank.interestrate.exception.InterestRateConflictException;
import com.los.bank.interestrate.exception.InterestRateNotFoundException;
import com.los.bank.interestrate.exception.InterestRateValidationException;
import com.los.bank.interestrate.repository.InterestRateMasterRepository;
import com.los.bank.product.entity.LoanProduct;
import com.los.bank.product.repository.LoanProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterestRateMasterServiceTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Mock private InterestRateMasterRepository rateMasterRepository;
    @Mock private LoanProductRepository loanProductRepository;

    @InjectMocks
    private InterestRateMasterServiceImpl service;

    private LoanProduct product;

    @BeforeEach
    void setUp() {
        product = LoanProduct.builder()
                .id(7L).code("PL01").name("Personal Loan").status("ACTIVE")
                .minAmount(new BigDecimal("50000")).maxAmount(new BigDecimal("1000000"))
                .minTenureMonths(6).maxTenureMonths(60)
                .penalty(true).penaltyPercent(new BigDecimal("2.0000"))
                .build();
        when(loanProductRepository.findByCodeIgnoreCase(argThat(code -> "PL01".equalsIgnoreCase(code))))
                .thenReturn(Optional.of(product));
        when(loanProductRepository.findById(7L)).thenReturn(Optional.of(product));
        when(rateMasterRepository.save(any(InterestRateMaster.class))).thenAnswer(inv -> {
            InterestRateMaster m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(100L);
            }
            return m;
        });
        when(rateMasterRepository.saveAndFlush(any(InterestRateMaster.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static InterestRateTierRequest tierReq(String amount, int months, String rate) {
        return InterestRateTierRequest.builder()
                .upToAmount(new BigDecimal(amount)).upToMonths(months).normalRate(new BigDecimal(rate)).build();
    }

    private InterestRateMasterRequest validRequest() {
        return InterestRateMasterRequest.builder()
                .productCode("PL01")
                .effectiveDate(TODAY.plusDays(5))
                .tiers(new ArrayList<>(List.of(
                        tierReq("1000000", 60, "11.75"),
                        tierReq("500000", 36, "12.50"))))
                .build();
    }

    private static InterestRateTier tier(long id, int srNo, String amount, int months, String rate) {
        return InterestRateTier.builder().id(id).srNo(srNo)
                .upToAmount(new BigDecimal(amount)).upToMonths(months).normalRate(new BigDecimal(rate)).build();
    }

    private InterestRateMaster sheet(String status, LocalDate effectiveDate, Long createdBy) {
        InterestRateMaster m = InterestRateMaster.builder()
                .id(100L).productId(7L).effectiveDate(effectiveDate).status(status).createdBy(createdBy).build();
        m.replaceTiers(new ArrayList<>(List.of(tier(1L, 1, "500000", 36, "12.50"))));
        when(rateMasterRepository.findById(100L)).thenReturn(Optional.of(m));
        return m;
    }

    // ---------------------------------------------------------------------
    // Create / validation
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Create saves a PENDING sheet with tiers sorted by amount then months")
    void testCreate() {
        InterestRateMasterResponse resp = service.create(validRequest(), 11L);

        assertEquals(InterestRateStatus.PENDING, resp.getStatus());
        assertEquals("PL01", resp.getProductCode());
        assertEquals("Personal Loan", resp.getProductName());
        assertEquals(11L, resp.getCreatedBy());
        assertTrue(resp.isEditable());
        assertFalse(resp.isCurrentlyApplied());
        assertEquals(2, resp.getTiers().size());
        assertEquals(1, resp.getTiers().get(0).getSrNo());
        assertEquals(new BigDecimal("500000"), resp.getTiers().get(0).getUpToAmount());
        assertEquals(new BigDecimal("1000000"), resp.getTiers().get(1).getUpToAmount());
        assertTrue(resp.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("Create rejects past effective date, unknown product, bad rates and duplicate tiers")
    void testCreateValidation() {
        InterestRateMasterRequest request = validRequest();
        request.setEffectiveDate(TODAY.minusDays(1));
        request.getTiers().add(tierReq("500000", 36, "13"));          // duplicate band
        request.getTiers().add(InterestRateTierRequest.builder()
                .upToAmount(new BigDecimal("200000")).upToMonths(12).normalRate(new BigDecimal("101")).build());
        request.getTiers().add(InterestRateTierRequest.builder()
                .upToAmount(new BigDecimal("300000")).upToMonths(500).build());

        InterestRateValidationException ex = assertThrows(InterestRateValidationException.class,
                () -> service.create(request, 11L));

        assertTrue(ex.getErrors().containsKey("effectiveDate"));
        assertTrue(ex.getErrors().containsKey("tiers[2].upToAmount"));
        assertEquals("Normal interest rate must be between 0 and 100", ex.getErrors().get("tiers[3].normalRate"));
        assertTrue(ex.getErrors().containsKey("tiers[4].upToMonths"));
        assertEquals("Normal interest rate is required", ex.getErrors().get("tiers[4].normalRate"));
        verify(rateMasterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rejects an unknown or inactive product")
    void testCreateProductChecks() {
        InterestRateMasterRequest request = validRequest();
        request.setProductCode("NOPE");
        InterestRateValidationException ex = assertThrows(InterestRateValidationException.class,
                () -> service.create(request, 11L));
        assertEquals("Loan product not found: NOPE", ex.getErrors().get("productCode"));

        product.setStatus("INACTIVE");
        InterestRateValidationException ex2 = assertThrows(InterestRateValidationException.class,
                () -> service.create(validRequest(), 11L));
        assertTrue(ex2.getErrors().get("productCode").contains("inactive"));
    }

    @Test
    @DisplayName("Create rejects a tier below the product minimum amount or tenure")
    void testCreateTierBelowProductMinimum() {
        InterestRateMasterRequest request = validRequest();
        request.getTiers().add(tierReq("40000", 3, "14"));

        InterestRateValidationException ex = assertThrows(InterestRateValidationException.class,
                () -> service.create(request, 11L));

        assertTrue(ex.getErrors().containsKey("tiers[2].upToAmount"));
        assertTrue(ex.getErrors().containsKey("tiers[2].upToMonths"));
    }

    @Test
    @DisplayName("Create rejects a second live sheet for the same product and date")
    void testCreateDuplicateDate() {
        when(rateMasterRepository.existsLiveSheet(eq(7L), any(), isNull())).thenReturn(true);

        assertThrows(InterestRateConflictException.class, () -> service.create(validRequest(), 11L));
    }

    @Test
    @DisplayName("Create warns when no tier covers the product maximum amount and tenure")
    void testCoverageWarning() {
        InterestRateMasterRequest request = validRequest();
        request.setTiers(new ArrayList<>(List.of(tierReq("500000", 36, "12.50"))));

        InterestRateMasterResponse resp = service.create(request, 11L);

        assertEquals(1, resp.getWarnings().size());
        assertTrue(resp.getWarnings().get(0).contains("1000000"));
    }

    @Test
    @DisplayName("Request accepts the snake_case payload sent by the current frontend screen")
    void testSnakeCasePayload() {
        String json = "{\"product_code\":\"PL01\",\"product_name\":\"Personal Loan\",\"effective_date\":\"2030-01-01\","
                + "\"tiers\":[{\"up_to_amount\":500000,\"up_to_months\":36,\"normal_interest_rate\":12.5,"
                + "\"penal_rate\":2,\"adhoc_rate\":null}]}";
        JsonMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        InterestRateMasterRequest request = mapper.readValue(json, InterestRateMasterRequest.class);

        assertEquals("PL01", request.getProductCode());
        assertEquals(LocalDate.of(2030, 1, 1), request.getEffectiveDate());
        assertEquals(new BigDecimal("12.5"), request.getTiers().get(0).getNormalRate());
        assertEquals(new BigDecimal("2"), request.getTiers().get(0).getPenalRate());
        assertEquals(36, request.getTiers().get(0).getUpToMonths());
    }

    // ---------------------------------------------------------------------
    // Edit / delete
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("A sheet that is approved and in effect cannot be edited or deleted")
    void testInEffectSheetLocked() {
        sheet(InterestRateStatus.APPROVED, TODAY, 11L);

        assertThrows(InterestRateConflictException.class, () -> service.update(100L, validRequest(), 11L));
        assertThrows(InterestRateConflictException.class, () -> service.delete(100L, 11L));
        verify(rateMasterRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Editing an approved future sheet sends it back to PENDING")
    void testEditFutureApprovedSheet() {
        InterestRateMaster m = sheet(InterestRateStatus.APPROVED, TODAY.plusDays(10), 11L);
        m.setReviewedBy(22L);

        InterestRateMasterResponse resp = service.update(100L, validRequest(), 33L);

        assertEquals(InterestRateStatus.PENDING, resp.getStatus());
        assertNull(resp.getReviewedBy());
        assertEquals(33L, resp.getModifiedBy());
        assertEquals(2, resp.getTiers().size());
        verify(rateMasterRepository).existsLiveSheet(eq(7L), any(), eq(100L));
    }

    @Test
    @DisplayName("A pending sheet can be deleted")
    void testDeletePending() {
        InterestRateMaster m = sheet(InterestRateStatus.PENDING, TODAY.plusDays(1), 11L);

        service.delete(100L, 11L);

        verify(rateMasterRepository).delete(m);
    }

    // ---------------------------------------------------------------------
    // Approve / reject
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("The maker cannot approve their own sheet")
    void testMakerCannotApprove() {
        sheet(InterestRateStatus.PENDING, TODAY.plusDays(1), 11L);

        assertThrows(InterestRateConflictException.class, () -> service.approve(100L, 11L));
    }

    @Test
    @DisplayName("The last editor counts as the maker")
    void testLastEditorCannotApprove() {
        InterestRateMaster m = sheet(InterestRateStatus.PENDING, TODAY.plusDays(1), 11L);
        m.setModifiedBy(33L);

        assertThrows(InterestRateConflictException.class, () -> service.approve(100L, 33L));
        assertDoesNotThrow(() -> service.approve(100L, 11L));
    }

    @Test
    @DisplayName("A different user approves: APPROVED with reviewer and time")
    void testApprove() {
        sheet(InterestRateStatus.PENDING, TODAY.plusDays(1), 11L);

        InterestRateMasterResponse resp = service.approve(100L, 22L);

        assertEquals(InterestRateStatus.APPROVED, resp.getStatus());
        assertEquals(22L, resp.getReviewedBy());
        assertNotNull(resp.getReviewedAt());
        assertTrue(resp.isEditable(), "Future-dated approved sheet stays editable until it takes effect");
    }

    @Test
    @DisplayName("Approve fails when the sheet is not pending or its date has passed")
    void testApproveStateChecks() {
        InterestRateMaster m = sheet(InterestRateStatus.REJECTED, TODAY.plusDays(1), 11L);
        assertThrows(InterestRateConflictException.class, () -> service.approve(100L, 22L));

        m.setStatus(InterestRateStatus.PENDING);
        m.setEffectiveDate(TODAY.minusDays(1));
        assertThrows(InterestRateValidationException.class, () -> service.approve(100L, 22L));
    }

    @Test
    @DisplayName("Reject needs a reason and stores it")
    void testReject() {
        sheet(InterestRateStatus.PENDING, TODAY.plusDays(1), 11L);

        assertThrows(InterestRateValidationException.class, () -> service.reject(100L, "  ", 22L));

        InterestRateMasterResponse resp = service.reject(100L, "Rates too low", 22L);
        assertEquals(InterestRateStatus.REJECTED, resp.getStatus());
        assertEquals("Rates too low", resp.getRejectionReason());
    }

    // ---------------------------------------------------------------------
    // Applicable rate
    // ---------------------------------------------------------------------

    private InterestRateMaster approvedGrid() {
        InterestRateMaster m = InterestRateMaster.builder()
                .id(100L).productId(7L).effectiveDate(TODAY.minusDays(10)).status(InterestRateStatus.APPROVED).build();
        InterestRateTier smallShort = tier(1L, 1, "500000", 36, "12.50");
        smallShort.setPenalRate(new BigDecimal("3.00"));
        m.replaceTiers(new ArrayList<>(List.of(
                smallShort,
                tier(2L, 2, "500000", 60, "13.00"),
                tier(3L, 3, "1000000", 36, "11.75"),
                tier(4L, 4, "1000000", 60, "12.25"))));
        when(rateMasterRepository.findFirstByProductIdAndStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                eq(7L), eq(InterestRateStatus.APPROVED), any())).thenReturn(Optional.of(m));
        return m;
    }

    @Test
    @DisplayName("Applicable rate picks the smallest tier covering amount and tenure (grid)")
    void testApplicableRateGrid() {
        approvedGrid();

        assertEquals(new BigDecimal("12.50"),
                service.findApplicableRate("PL01", new BigDecimal("400000"), 24, null).getNormalRate());
        assertEquals(new BigDecimal("13.00"),
                service.findApplicableRate("PL01", new BigDecimal("400000"), 48, null).getNormalRate());
        assertEquals(new BigDecimal("11.75"),
                service.findApplicableRate("PL01", new BigDecimal("500000.01"), 36, null).getNormalRate());
        ApplicableRateResponse big = service.findApplicableRate("pl01", new BigDecimal("1000000"), 60, null);
        assertEquals(new BigDecimal("12.25"), big.getNormalRate());
        assertEquals(4L, big.getTierId());
        assertEquals(100L, big.getRateMasterId());
        assertEquals(TODAY, big.getAsOfDate());
    }

    @Test
    @DisplayName("Penal rate comes from the tier, else from the product penalty")
    void testPenalRateSource() {
        approvedGrid();

        ApplicableRateResponse fromTier = service.findApplicableRate("PL01", new BigDecimal("100000"), 12, null);
        assertEquals(new BigDecimal("3.00"), fromTier.getPenalRate());
        assertEquals("TIER", fromTier.getPenalRateSource());

        ApplicableRateResponse fromProduct = service.findApplicableRate("PL01", new BigDecimal("800000"), 12, null);
        assertEquals(new BigDecimal("2.0000"), fromProduct.getPenalRate());
        assertEquals("PRODUCT", fromProduct.getPenalRateSource());
    }

    @Test
    @DisplayName("Staircase tiers: a loan that does not fit the small tier uses the next one that covers both")
    void testSelectTierStaircase() {
        List<InterestRateTier> tiers = List.of(tier(1L, 1, "500000", 36, "12"), tier(2L, 2, "1000000", 60, "11"));

        assertEquals(1L, InterestRateMasterServiceImpl.selectTier(tiers, new BigDecimal("300000"), 24).orElseThrow().getId());
        assertEquals(2L, InterestRateMasterServiceImpl.selectTier(tiers, new BigDecimal("300000"), 48).orElseThrow().getId());
        assertTrue(InterestRateMasterServiceImpl.selectTier(tiers, new BigDecimal("1200000"), 12).isEmpty());
    }

    @Test
    @DisplayName("Applicable rate: outside product limits is 400, no sheet or no tier is 404")
    void testApplicableRateErrors() {
        InterestRateValidationException limits = assertThrows(InterestRateValidationException.class,
                () -> service.findApplicableRate("PL01", new BigDecimal("2000000"), 72, null));
        assertTrue(limits.getErrors().containsKey("amount"));
        assertTrue(limits.getErrors().containsKey("tenureMonths"));

        InterestRateNotFoundException noSheet = assertThrows(InterestRateNotFoundException.class,
                () -> service.findApplicableRate("PL01", new BigDecimal("100000"), 12, null));
        assertTrue(noSheet.getMessage().contains("No approved interest rate sheet"));

        InterestRateMaster m = approvedGrid();
        m.replaceTiers(new ArrayList<>(List.of(tier(1L, 1, "500000", 36, "12.50"))));
        assertThrows(InterestRateNotFoundException.class,
                () -> service.findApplicableRate("PL01", new BigDecimal("900000"), 12, null));
    }

    @Test
    @DisplayName("Applicable rate looks up the sheet in effect on the given date")
    void testApplicableRateOnDate() {
        approvedGrid();
        LocalDate date = LocalDate.of(2027, 3, 1);

        ApplicableRateResponse resp = service.findApplicableRate("PL01", new BigDecimal("100000"), 12, date);

        assertEquals(date, resp.getAsOfDate());
        verify(rateMasterRepository).findFirstByProductIdAndStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                7L, InterestRateStatus.APPROVED, date);
    }
}
