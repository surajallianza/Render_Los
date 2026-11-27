package com.los.bank.lookup.service;

import com.los.bank.lookup.cache.LookupCacheManager;
import com.los.bank.lookup.dto.*;
import com.los.bank.lookup.entity.Lookup;
import com.los.bank.lookup.entity.SubLookup;
import com.los.bank.lookup.exception.LookupBadRequestException;
import com.los.bank.lookup.exception.LookupConflictException;
import com.los.bank.lookup.exception.LookupNotFoundException;
import com.los.bank.lookup.repository.LookupRepository;
import com.los.bank.lookup.repository.SubLookupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LookupServiceTest {

    @Mock
    private LookupRepository lookupRepository;

    @Mock
    private SubLookupRepository subLookupRepository;

    @Mock
    private LookupCacheManager cacheManager;

    @InjectMocks
    private LookupService lookupService;

    private Lookup normalLookup;
    private Lookup fixedLookup;
    private SubLookup normalSubLookup;
    private SubLookup fixedSubLookup;

    @BeforeEach
    void setUp() {
        normalLookup = Lookup.builder()
                .id(1L)
                .code("10001")
                .description("Customer Category")
                .isFixed(false)
                .isActive(true)
                .build();

        fixedLookup = Lookup.builder()
                .id(2L)
                .code("10002")
                .description("System Critical Lookup")
                .isFixed(true)
                .isActive(true)
                .build();

        normalSubLookup = SubLookup.builder()
                .id(10L)
                .lookupTypeCode("10001")
                .subTypeCode("1")
                .subTypeDescription("Individual")
                .isFixed(false)
                .isActive(true)
                .displayOrder(1)
                .build();

        fixedSubLookup = SubLookup.builder()
                .id(20L)
                .lookupTypeCode("10002")
                .subTypeCode("1")
                .subTypeDescription("Permanent System Flag")
                .isFixed(true)
                .isActive(true)
                .displayOrder(1)
                .build();
    }

    // =========================================================================
    // 1. DUPLICATE CODE TESTS
    // =========================================================================

    @Test
    @DisplayName("createLookup: throws LookupConflictException when lookup code already exists")
    void createLookup_duplicateCode_throwsLookupConflictException() {
        LookupCreateRequest request = LookupCreateRequest.builder()
                .code("10001")
                .name("Duplicate Lookup")
                .build();

        when(lookupRepository.existsByCode("10001")).thenReturn(true);

        LookupConflictException ex = assertThrows(LookupConflictException.class, () ->
                lookupService.createLookup(request));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(lookupRepository, never()).save(any());
        verify(cacheManager, never()).evictLookup(any());
    }

    @Test
    @DisplayName("createSubLookup: throws LookupConflictException when sub lookup code already exists in same parent lookup")
    void createSubLookup_duplicateCodeInSameParent_throwsLookupConflictException() {
        SubLookupCreateRequest request = SubLookupCreateRequest.builder()
                .code("1")
                .subLookupCode("1000101")
                .name("Individual")
                .build();

        when(lookupRepository.findById(1L)).thenReturn(Optional.of(normalLookup));
        when(subLookupRepository.existsByLookupTypeCodeAndSubTypeCode("10001", "1000101")).thenReturn(true);

        LookupConflictException ex = assertThrows(LookupConflictException.class, () ->
                lookupService.createSubLookup(1L, request));

        assertTrue(ex.getMessage().contains("already exists under lookup '10001'"));
        verify(subLookupRepository, never()).save(any());
    }

    // =========================================================================
    // 2. FIXED-RECORD PROTECTION TESTS
    // =========================================================================

    @Test
    @DisplayName("updateLookup: throws LookupBadRequestException when trying to deactivate fixed lookup")
    void updateLookup_deactivateFixedLookup_throwsLookupBadRequestException() {
        LookupUpdateRequest updateRequest = LookupUpdateRequest.builder()
                .status("Inactive")
                .build();

        when(lookupRepository.findById(2L)).thenReturn(Optional.of(fixedLookup));

        LookupBadRequestException ex = assertThrows(LookupBadRequestException.class, () ->
                lookupService.updateLookup(2L, updateRequest));

        assertTrue(ex.getMessage().contains("cannot be deactivated"));
        verify(lookupRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleLookupStatus: throws LookupBadRequestException when deactivating fixed lookup")
    void toggleLookupStatus_deactivateFixedLookup_throwsLookupBadRequestException() {
        StatusUpdateRequest request = StatusUpdateRequest.builder()
                .status("Inactive")
                .build();

        when(lookupRepository.findById(2L)).thenReturn(Optional.of(fixedLookup));

        LookupBadRequestException ex = assertThrows(LookupBadRequestException.class, () ->
                lookupService.toggleLookupStatus(2L, request));

        assertTrue(ex.getMessage().contains("cannot be deactivated"));
        verify(lookupRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateLookup: allows name and description edits on fixed lookup")
    void updateLookup_fixedLookup_allowsNameEdit() {
        LookupUpdateRequest updateRequest = LookupUpdateRequest.builder()
                .name("Updated Fixed Description")
                .status("Active")
                .build();

        when(lookupRepository.findById(2L)).thenReturn(Optional.of(fixedLookup));
        when(lookupRepository.save(any(Lookup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LookupResponse response = lookupService.updateLookup(2L, updateRequest);

        assertNotNull(response);
        assertEquals("Updated Fixed Description", response.getName());
        verify(cacheManager).evictLookup("10002");
    }

    @Test
    @DisplayName("updateSubLookup: throws LookupBadRequestException when trying to deactivate fixed sub lookup")
    void updateSubLookup_deactivateFixedSubLookup_throwsLookupBadRequestException() {
        SubLookupUpdateRequest request = SubLookupUpdateRequest.builder()
                .status("Inactive")
                .build();

        when(subLookupRepository.findById(20L)).thenReturn(Optional.of(fixedSubLookup));

        LookupBadRequestException ex = assertThrows(LookupBadRequestException.class, () ->
                lookupService.updateSubLookup(20L, request));

        assertTrue(ex.getMessage().contains("cannot be deactivated"));
        verify(subLookupRepository, never()).save(any());
    }

    // =========================================================================
    // 3. STATUS TOGGLE TESTS
    // =========================================================================

    @Test
    @DisplayName("toggleLookupStatus: successfully deactivates and activates non-fixed lookup")
    void toggleLookupStatus_success() {
        when(lookupRepository.findById(1L)).thenReturn(Optional.of(normalLookup));
        when(lookupRepository.save(any(Lookup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Toggle to Inactive
        StatusUpdateRequest deactiveReq = StatusUpdateRequest.builder().status("Inactive").build();
        LookupResponse inactiveResp = lookupService.toggleLookupStatus(1L, deactiveReq);
        assertEquals("Inactive", inactiveResp.getStatus());

        // Toggle to Active
        StatusUpdateRequest activeReq = StatusUpdateRequest.builder().status("Active").build();
        LookupResponse activeResp = lookupService.toggleLookupStatus(1L, activeReq);
        assertEquals("Active", activeResp.getStatus());

        verify(cacheManager, times(2)).evictLookup("10001");
    }

    @Test
    @DisplayName("toggleSubLookupStatus: successfully toggles non-fixed sub lookup")
    void toggleSubLookupStatus_success() {
        when(subLookupRepository.findById(10L)).thenReturn(Optional.of(normalSubLookup));
        when(subLookupRepository.save(any(SubLookup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StatusUpdateRequest deactiveReq = StatusUpdateRequest.builder().status("Inactive").build();
        SubLookupResponse response = lookupService.toggleSubLookupStatus(10L, deactiveReq);

        assertEquals("Inactive", response.getStatus());
        verify(cacheManager).evictLookup("10001");
    }

    @Test
    @DisplayName("createSubLookup: auto-increments display order when not supplied")
    void createSubLookup_autoIncrementsDisplayOrder() {
        SubLookupCreateRequest request = SubLookupCreateRequest.builder()
                .code("1")
                .subLookupCode("1000102")
                .name("Corporate")
                .build();

        when(lookupRepository.findById(1L)).thenReturn(Optional.of(normalLookup));
        when(subLookupRepository.existsByLookupTypeCodeAndSubTypeCode("10001", "1000102")).thenReturn(false);
        when(subLookupRepository.findMaxDisplayOrderByLookupTypeCode("10001")).thenReturn(3);
        when(subLookupRepository.save(any(SubLookup.class))).thenAnswer(invocation -> {
            SubLookup s = invocation.getArgument(0);
            s.setId(11L);
            return s;
        });

        SubLookupResponse response = lookupService.createSubLookup(1L, request);

        assertNotNull(response);
        assertEquals(4, response.getDisplayOrder());
        assertEquals("1000102", response.getSubLookupCode());
        verify(cacheManager).evictLookup("10001");
    }

    @Test
    @DisplayName("createSubLookup: throws LookupBadRequestException when sub lookup code does not start with parent lookup code")
    void createSubLookup_invalidPrefix_throwsLookupBadRequestException() {
        SubLookupCreateRequest request = SubLookupCreateRequest.builder()
                .code("1")
                .subLookupCode("9999901")
                .name("Invalid Prefix")
                .build();

        when(lookupRepository.findById(1L)).thenReturn(Optional.of(normalLookup));

        LookupBadRequestException ex = assertThrows(LookupBadRequestException.class, () ->
                lookupService.createSubLookup(1L, request));

        assertTrue(ex.getMessage().contains("must start with parent lookup code '10001'"));
        verify(subLookupRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSubLookup: creates sub lookup resolving parent from request body code and subLookupCode format <lookup><subcode>")
    void createSubLookup_directPayload_success() {
        SubLookupCreateRequest request = SubLookupCreateRequest.builder()
                .code("1")
                .subLookupCode("1000101")
                .name("Individual")
                .displayOrder(1)
                .build();

        when(lookupRepository.findById(1L)).thenReturn(Optional.of(normalLookup));
        when(subLookupRepository.existsByLookupTypeCodeAndSubTypeCode("10001", "1000101")).thenReturn(false);
        when(subLookupRepository.save(any(SubLookup.class))).thenAnswer(invocation -> {
            SubLookup s = invocation.getArgument(0);
            s.setId(15L);
            return s;
        });

        SubLookupResponse response = lookupService.createSubLookup((String) null, request);

        assertNotNull(response);
        assertEquals("1000101", response.getSubLookupCode());
        assertEquals("Individual", response.getName());
    }
}
