package com.los.bank.goldrate;

import com.los.bank.goldrate.dto.GoldRateActivityDto;
import com.los.bank.goldrate.dto.GoldRateMasterDto;
import com.los.bank.goldrate.exception.GoldRateException;
import com.los.bank.goldrate.service.GoldRateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GoldRateServiceTest {

    private GoldRateServiceImpl service;

    @BeforeEach
    void setUp() {
        // Pure unit test without heavy agent attachments
        service = new GoldRateServiceImpl(null, null, null, null);
    }

    @Test
    @DisplayName("Test 1: Retrieve All Activities returns reference record with all 18 fields")
    void testGetAllActivities() {
        List<GoldRateActivityDto> activities = service.getAllActivities(null, null);

        assertNotNull(activities);
        assertFalse(activities.isEmpty());

        GoldRateActivityDto sample = activities.get(0);
        assertEquals("12", sample.getId());
        assertEquals("26 Oct 2026", sample.getDate());
        assertEquals("11:45 AM", sample.getTime());
        assertEquals("Gold Rate Master", sample.getModule());
        assertEquals("GOLD-1029-12", sample.getRecordId());
        assertEquals("Updated", sample.getAction());
        assertEquals("22K Rate", sample.getField());
        assertEquals("₹6,420", sample.getOldValue());
        assertEquals("₹6,455", sample.getNewValue());
        assertEquals("Neha Singh", sample.getCreatedBy());
        assertEquals("USR-108", sample.getCreatedById());
        assertEquals("Neha Singh", sample.getModifiedBy());
        assertEquals("USR-108", sample.getModifiedById());
        assertEquals("-", sample.getVerifiedBy());
        assertEquals("-", sample.getVerifiedById());
        assertEquals("Pending", sample.getStatus());
        assertEquals("Daily gold rate was updated for loan valuation.", sample.getDescription());
        assertEquals("192.168.1.21", sample.getIp());
    }

    @Test
    @DisplayName("Test 2: Retrieve Activity by valid ID")
    void testGetActivityById_Success() {
        GoldRateActivityDto dto = service.getActivityById("12");

        assertNotNull(dto);
        assertEquals("12", dto.getId());
        assertEquals("GOLD-1029-12", dto.getRecordId());
        assertEquals("22K Rate", dto.getField());
    }

    @Test
    @DisplayName("Test 3: Retrieve Activity by non-existent ID throws 404")
    void testGetActivityById_NotFound() {
        assertThrows(GoldRateException.class, () -> service.getActivityById("NON_EXISTENT_999"));
    }

    @Test
    @DisplayName("Test 4: Create Activity record captures client IP and dates correctly")
    void testCreateActivity_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");

        GoldRateActivityDto createDto = GoldRateActivityDto.builder()
                .module("Gold Rate Master")
                .recordId("GOLD-1029-13")
                .action("Created")
                .field("24K Rate")
                .oldValue("-")
                .newValue("₹7,040")
                .createdBy("Neha Singh")
                .createdById("USR-108")
                .status("Pending")
                .description("Initial 24K rate set.")
                .build();

        GoldRateActivityDto created = service.createActivity(createDto, request);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("GOLD-1029-13", created.getRecordId());
        assertEquals("10.0.0.5", created.getIp());
        assertEquals("Pending", created.getStatus());
    }

    @Test
    @DisplayName("Test 5: Create Activity validates mandatory fields")
    void testCreateActivity_Validation() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        GoldRateActivityDto invalidDto = GoldRateActivityDto.builder().build();

        assertThrows(GoldRateException.class, () -> service.createActivity(invalidDto, request));
    }

    @Test
    @DisplayName("Test 6: Update Activity supports maker-checker verification while preserving immutable audit details")
    void testUpdateActivity_MakerChecker() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        GoldRateActivityDto updateDto = GoldRateActivityDto.builder()
                .status("Approved")
                .verifiedBy("Rajesh Verma")
                .verifiedById("USR-201")
                .description("Verified by Branch Manager.")
                // Attempt to modify immutable fields
                .oldValue("₹1,000")
                .newValue("₹2,000")
                .build();

        GoldRateActivityDto updated = service.updateActivity("12", updateDto, request);

        assertNotNull(updated);
        assertEquals("12", updated.getId());
        assertEquals("Approved", updated.getStatus());
        assertEquals("Rajesh Verma", updated.getVerifiedBy());
        assertEquals("USR-201", updated.getVerifiedById());
        // Verify immutable historical fields are preserved!
        assertEquals("₹6,420", updated.getOldValue());
        assertEquals("₹6,455", updated.getNewValue());
        assertEquals("22K Rate", updated.getField());
    }

    @Test
    @DisplayName("Test 7: Master Rate Update triggers automatic Audit Trail with accurate Old and New values")
    void testMasterRateUpdate_AutoAuditTrail() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.55");

        // Existing rate is ₹6,455 for 22K (from setup)
        GoldRateMasterDto updateDto = GoldRateMasterDto.builder()
                .rate22k(new BigDecimal("6500.00"))
                .rate24k(new BigDecimal("7040.00"))
                .modifiedBy("Neha Singh")
                .modifiedById("USR-108")
                .description("Daily gold rate updated for market revision.")
                .build();

        GoldRateMasterDto masterResult = service.updateMasterRate("GOLD-1029-12", updateDto, request);
        assertNotNull(masterResult);
        assertEquals(new BigDecimal("6500.00"), masterResult.getRate22k());

        // Check that an audit activity was automatically created with old and new values
        List<GoldRateActivityDto> activities = service.getAllActivities("GOLD-1029-12", null);
        assertFalse(activities.isEmpty());

        GoldRateActivityDto latest = activities.stream()
                .filter(a -> "₹6,500".equals(a.getNewValue()))
                .findFirst()
                .orElse(null);

        assertNotNull(latest, "Expected audit entry with new value ₹6,500");
        assertEquals("₹6,455", latest.getOldValue());
        assertEquals("22K Rate", latest.getField());
        assertEquals("Updated", latest.getAction());
        assertEquals("192.168.1.55", latest.getIp());
    }
}
