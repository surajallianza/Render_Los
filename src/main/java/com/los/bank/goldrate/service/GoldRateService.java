package com.los.bank.goldrate.service;

import com.los.bank.goldrate.dto.GoldRateActivityDto;
import com.los.bank.goldrate.dto.GoldRateMasterDto;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Service interface for Gold Rate Master and Audit Trail activities.
 */
public interface GoldRateService {

    // --- Activity / Audit Trail Operations ---

    List<GoldRateActivityDto> getAllActivities(String recordId, String status);

    GoldRateActivityDto getActivityById(String id);

    GoldRateActivityDto createActivity(GoldRateActivityDto requestDto, HttpServletRequest request);

    GoldRateActivityDto updateActivity(String id, GoldRateActivityDto requestDto, HttpServletRequest request);

    // --- Gold Rate Master Record Operations ---

    List<GoldRateMasterDto> getAllMasterRates();

    GoldRateMasterDto getMasterRateByRecordId(String recordId);

    GoldRateMasterDto createMasterRate(GoldRateMasterDto requestDto, HttpServletRequest request);

    GoldRateMasterDto updateMasterRate(String recordId, GoldRateMasterDto requestDto, HttpServletRequest request);
}
