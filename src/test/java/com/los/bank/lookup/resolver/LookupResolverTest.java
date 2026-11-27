package com.los.bank.lookup.resolver;

import com.los.bank.lookup.cache.LookupCacheManager;
import com.los.bank.lookup.entity.Lookup;
import com.los.bank.lookup.entity.SubLookup;
import com.los.bank.lookup.exception.LookupBadRequestException;
import com.los.bank.lookup.repository.LookupRepository;
import com.los.bank.lookup.repository.SubLookupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LookupResolverTest {

    @Mock
    private LookupRepository lookupRepository;

    @Mock
    private SubLookupRepository subLookupRepository;

    @Mock
    private LookupCacheManager cacheManager;

    @InjectMocks
    private LookupResolver lookupResolver;

    private Lookup activeLoanTypeLookup;
    private Lookup inactiveLookup;
    private SubLookup activeGoldLoan;
    private SubLookup inactiveSubLookup;

    @BeforeEach
    void setUp() {
        activeLoanTypeLookup = Lookup.builder()
                .id(1L)
                .code("10004")
                .description("Loan Types")
                .isActive(true)
                .isFixed(false)
                .build();

        inactiveLookup = Lookup.builder()
                .id(2L)
                .code("10009")
                .description("Legacy Deprecated Lookup")
                .isActive(false)
                .isFixed(false)
                .build();

        activeGoldLoan = SubLookup.builder()
                .id(101L)
                .lookupTypeCode("10004")
                .subTypeCode("1")
                .subTypeDescription("Gold Loan")
                .isActive(true)
                .isFixed(false)
                .displayOrder(1)
                .build();

        inactiveSubLookup = SubLookup.builder()
                .id(102L)
                .lookupTypeCode("10004")
                .subTypeCode("5")
                .subTypeDescription("Discontinued Loan")
                .isActive(false)
                .isFixed(false)
                .displayOrder(5)
                .build();
    }

    // =========================================================================
    // 1. SUCCESSFUL RESOLUTION
    // =========================================================================

    @Test
    @DisplayName("resolveId: successfully resolves valid (lookupCode, subCode) to m002.id")
    void resolveId_success() {
        when(cacheManager.getResolvedId("10004", "1")).thenReturn(null);
        when(lookupRepository.findByCode("10004")).thenReturn(Optional.of(activeLoanTypeLookup));
        when(subLookupRepository.findByLookupTypeCodeAndSubTypeCode("10004", "1")).thenReturn(Optional.of(activeGoldLoan));

        Long resolvedId = lookupResolver.resolveId("10004", "1");

        assertEquals(101L, resolvedId);
        verify(cacheManager).putResolvedId("10004", "1", 101L);
        verify(cacheManager).putResolvedName("10004", "1", "Gold Loan");
    }

    // =========================================================================
    // 2. CODE BELONGS TO A DIFFERENT LOOKUP / DOES NOT EXIST
    // =========================================================================

    @Test
    @DisplayName("resolveId: throws LookupBadRequestException when sub code belongs to different lookup or does not exist")
    void resolveId_subCodeBelongsToDifferentLookup_throwsLookupBadRequestException() {
        // Looking for subCode "99" in lookup "10004" (it does not exist under 10004)
        when(cacheManager.getResolvedId("10004", "99")).thenReturn(null);
        when(lookupRepository.findByCode("10004")).thenReturn(Optional.of(activeLoanTypeLookup));
        when(subLookupRepository.findByLookupTypeCodeAndSubTypeCode("10004", "99")).thenReturn(Optional.empty());

        LookupBadRequestException ex = assertThrows(LookupBadRequestException.class, () ->
                lookupResolver.resolveId("10004", "99"));

        assertTrue(ex.getMessage().contains("Invalid or inactive value '99' for lookup 10004"));
    }

    // =========================================================================
    // 3. INACTIVE SUB LOOKUP
    // =========================================================================

    @Test
    @DisplayName("resolveId: throws LookupBadRequestException when sub lookup is inactive")
    void resolveId_inactiveSubLookup_throwsLookupBadRequestException() {
        when(cacheManager.getResolvedId("10004", "5")).thenReturn(null);
        when(lookupRepository.findByCode("10004")).thenReturn(Optional.of(activeLoanTypeLookup));
        when(subLookupRepository.findByLookupTypeCodeAndSubTypeCode("10004", "5")).thenReturn(Optional.of(inactiveSubLookup));

        LookupBadRequestException ex = assertThrows(LookupBadRequestException.class, () ->
                lookupResolver.resolveId("10004", "5"));

        assertTrue(ex.getMessage().contains("Invalid or inactive value '5' for lookup 10004"));
    }

    // =========================================================================
    // 4. INACTIVE PARENT LOOKUP
    // =========================================================================

    @Test
    @DisplayName("resolveId: throws LookupBadRequestException when parent lookup is inactive")
    void resolveId_inactiveParentLookup_throwsLookupBadRequestException() {
        when(cacheManager.getResolvedId("10009", "1")).thenReturn(null);
        when(lookupRepository.findByCode("10009")).thenReturn(Optional.of(inactiveLookup));

        LookupBadRequestException ex = assertThrows(LookupBadRequestException.class, () ->
                lookupResolver.resolveId("10009", "1"));

        assertTrue(ex.getMessage().contains("Invalid or inactive value '1' for lookup 10009"));
    }

    // =========================================================================
    // 5. IS VALID CHECKS
    // =========================================================================

    @Test
    @DisplayName("isValid: returns true for active lookup and active sub lookup")
    void isValid_returnsTrue() {
        when(cacheManager.getValidity("10004", "1")).thenReturn(null);
        when(lookupRepository.findByCode("10004")).thenReturn(Optional.of(activeLoanTypeLookup));
        when(subLookupRepository.findByLookupTypeCodeAndSubTypeCode("10004", "1")).thenReturn(Optional.of(activeGoldLoan));

        assertTrue(lookupResolver.isValid("10004", "1"));
        verify(cacheManager).putValidity("10004", "1", true);
    }

    @Test
    @DisplayName("isValid: returns false without throwing exception for invalid/inactive code")
    void isValid_returnsFalseForInvalid() {
        when(cacheManager.getValidity("10004", "999")).thenReturn(null);
        when(lookupRepository.findByCode("10004")).thenReturn(Optional.of(activeLoanTypeLookup));
        when(subLookupRepository.findByLookupTypeCodeAndSubTypeCode("10004", "999")).thenReturn(Optional.empty());

        assertFalse(lookupResolver.isValid("10004", "999"));
        verify(cacheManager).putValidity("10004", "999", false);
    }

    // =========================================================================
    // 6. BATCH RESOLUTION TEST (N+1 PREVENTION)
    // =========================================================================

    @Test
    @DisplayName("resolveBatch: resolves multiple sub codes in single query")
    void resolveBatch_success() {
        SubLookup personalLoan = SubLookup.builder()
                .id(103L)
                .lookupTypeCode("10004")
                .subTypeCode("2")
                .subTypeDescription("Personal Loan")
                .isActive(true)
                .build();

        when(lookupRepository.findByCode("10004")).thenReturn(Optional.of(activeLoanTypeLookup));
        when(subLookupRepository.findByLookupTypeCodeAndIsActiveTrueOrderByDisplayOrderAscSubTypeCodeAsc("10004"))
                .thenReturn(Arrays.asList(activeGoldLoan, personalLoan));

        Map<String, Long> batchMap = lookupResolver.resolveBatch("10004", Arrays.asList("1", "2"));

        assertEquals(2, batchMap.size());
        assertEquals(101L, batchMap.get("1"));
        assertEquals(103L, batchMap.get("2"));
    }

    @Test
    @DisplayName("resolveId: successfully resolves formatted <lookupCode><subCode> e.g. 1000401")
    void resolveId_formattedCode_success() {
        SubLookup formattedSubLookup = SubLookup.builder()
                .id(105L)
                .lookupTypeCode("10004")
                .subTypeCode("1000401")
                .subTypeDescription("Gold Loan Formatted")
                .isActive(true)
                .build();

        when(cacheManager.getResolvedId("10004", "1000401")).thenReturn(null);
        when(lookupRepository.findByCode("10004")).thenReturn(Optional.of(activeLoanTypeLookup));
        when(subLookupRepository.findByLookupTypeCodeAndSubTypeCode("10004", "1000401")).thenReturn(Optional.of(formattedSubLookup));

        Long resolvedId = lookupResolver.resolveId("10004", "1000401");

        assertEquals(105L, resolvedId);
        verify(cacheManager).putResolvedId("10004", "1000401", 105L);
    }

    @Test
    @DisplayName("resolveId: successfully resolves relative '01' to formatted '1000401'")
    void resolveId_relativeCodeToFormatted_success() {
        SubLookup formattedSubLookup = SubLookup.builder()
                .id(105L)
                .lookupTypeCode("10004")
                .subTypeCode("1000401")
                .subTypeDescription("Gold Loan Formatted")
                .isActive(true)
                .build();

        when(cacheManager.getResolvedId("10004", "01")).thenReturn(null);
        when(lookupRepository.findByCode("10004")).thenReturn(Optional.of(activeLoanTypeLookup));
        when(subLookupRepository.findByLookupTypeCodeAndSubTypeCode("10004", "01")).thenReturn(Optional.empty());
        when(subLookupRepository.findByLookupTypeCodeAndSubTypeCode("10004", "1000401")).thenReturn(Optional.of(formattedSubLookup));

        Long resolvedId = lookupResolver.resolveId("10004", "01");

        assertEquals(105L, resolvedId);
        verify(cacheManager).putResolvedId("10004", "01", 105L);
        verify(cacheManager).putResolvedId("10004", "1000401", 105L);
    }
}
