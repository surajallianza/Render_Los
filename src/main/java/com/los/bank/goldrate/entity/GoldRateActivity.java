package com.los.bank.goldrate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity mapping table g011 (Gold Rate Activity / Complete Audit Trail).
 * Maintains historical, tamper-evident audit trail of Gold Rate Master changes.
 */
@Entity
@Table(name = "g011")
public class GoldRateActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gold_rate_id")
    private Long goldRateId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "activity_time", nullable = false)
    private LocalTime activityTime;

    @Column(name = "module", nullable = false, length = 100)
    private String module;

    @Column(name = "record_id", nullable = false, length = 50)
    private String recordId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "old_value", length = 100)
    private String oldValue;

    @Column(name = "new_value", length = 100)
    private String newValue;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_name", length = 100)
    private String createdByName;

    @Column(name = "created_by_emp_id", length = 50)
    private String createdByEmpId;

    @Column(name = "modified_by")
    private Long modifiedBy;

    @Column(name = "modified_by_name", length = 100)
    private String modifiedByName;

    @Column(name = "modified_by_emp_id", length = 50)
    private String modifiedByEmpId;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_by_name", length = 100)
    private String verifiedByName;

    @Column(name = "verified_by_emp_id", length = 50)
    private String verifiedByEmpId;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "status_lookup_id")
    private Long statusLookupId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public GoldRateActivity() {
    }

    public GoldRateActivity(Long id, Long goldRateId, LocalDate activityDate, LocalTime activityTime,
                            String module, String recordId, String action, String fieldName,
                            String oldValue, String newValue, Long createdBy, String createdByName,
                            String createdByEmpId, Long modifiedBy, String modifiedByName,
                            String modifiedByEmpId, Long verifiedBy, String verifiedByName,
                            String verifiedByEmpId, String status, Long statusLookupId,
                            String description, String ipAddress, LocalDateTime createdAt) {
        this.id = id;
        this.goldRateId = goldRateId;
        this.activityDate = activityDate;
        this.activityTime = activityTime;
        this.module = module;
        this.recordId = recordId;
        this.action = action;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdByEmpId = createdByEmpId;
        this.modifiedBy = modifiedBy;
        this.modifiedByName = modifiedByName;
        this.modifiedByEmpId = modifiedByEmpId;
        this.verifiedBy = verifiedBy;
        this.verifiedByName = verifiedByName;
        this.verifiedByEmpId = verifiedByEmpId;
        this.status = status;
        this.statusLookupId = statusLookupId;
        this.description = description;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public static GoldRateActivityBuilder builder() {
        return new GoldRateActivityBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGoldRateId() {
        return goldRateId;
    }

    public void setGoldRateId(Long goldRateId) {
        this.goldRateId = goldRateId;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public LocalTime getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(LocalTime activityTime) {
        this.activityTime = activityTime;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByEmpId() {
        return createdByEmpId;
    }

    public void setCreatedByEmpId(String createdByEmpId) {
        this.createdByEmpId = createdByEmpId;
    }

    public Long getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getModifiedByName() {
        return modifiedByName;
    }

    public void setModifiedByName(String modifiedByName) {
        this.modifiedByName = modifiedByName;
    }

    public String getModifiedByEmpId() {
        return modifiedByEmpId;
    }

    public void setModifiedByEmpId(String modifiedByEmpId) {
        this.modifiedByEmpId = modifiedByEmpId;
    }

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Long verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getVerifiedByName() {
        return verifiedByName;
    }

    public void setVerifiedByName(String verifiedByName) {
        this.verifiedByName = verifiedByName;
    }

    public String getVerifiedByEmpId() {
        return verifiedByEmpId;
    }

    public void setVerifiedByEmpId(String verifiedByEmpId) {
        this.verifiedByEmpId = verifiedByEmpId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getStatusLookupId() {
        return statusLookupId;
    }

    public void setStatusLookupId(Long statusLookupId) {
        this.statusLookupId = statusLookupId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class GoldRateActivityBuilder {
        private Long id;
        private Long goldRateId;
        private LocalDate activityDate;
        private LocalTime activityTime;
        private String module;
        private String recordId;
        private String action;
        private String fieldName;
        private String oldValue;
        private String newValue;
        private Long createdBy;
        private String createdByName;
        private String createdByEmpId;
        private Long modifiedBy;
        private String modifiedByName;
        private String modifiedByEmpId;
        private Long verifiedBy;
        private String verifiedByName;
        private String verifiedByEmpId;
        private String status;
        private Long statusLookupId;
        private String description;
        private String ipAddress;
        private LocalDateTime createdAt;

        GoldRateActivityBuilder() {
        }

        public GoldRateActivityBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public GoldRateActivityBuilder goldRateId(Long goldRateId) {
            this.goldRateId = goldRateId;
            return this;
        }

        public GoldRateActivityBuilder activityDate(LocalDate activityDate) {
            this.activityDate = activityDate;
            return this;
        }

        public GoldRateActivityBuilder activityTime(LocalTime activityTime) {
            this.activityTime = activityTime;
            return this;
        }

        public GoldRateActivityBuilder module(String module) {
            this.module = module;
            return this;
        }

        public GoldRateActivityBuilder recordId(String recordId) {
            this.recordId = recordId;
            return this;
        }

        public GoldRateActivityBuilder action(String action) {
            this.action = action;
            return this;
        }

        public GoldRateActivityBuilder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public GoldRateActivityBuilder oldValue(String oldValue) {
            this.oldValue = oldValue;
            return this;
        }

        public GoldRateActivityBuilder newValue(String newValue) {
            this.newValue = newValue;
            return this;
        }

        public GoldRateActivityBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public GoldRateActivityBuilder createdByName(String createdByName) {
            this.createdByName = createdByName;
            return this;
        }

        public GoldRateActivityBuilder createdByEmpId(String createdByEmpId) {
            this.createdByEmpId = createdByEmpId;
            return this;
        }

        public GoldRateActivityBuilder modifiedBy(Long modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        public GoldRateActivityBuilder modifiedByName(String modifiedByName) {
            this.modifiedByName = modifiedByName;
            return this;
        }

        public GoldRateActivityBuilder modifiedByEmpId(String modifiedByEmpId) {
            this.modifiedByEmpId = modifiedByEmpId;
            return this;
        }

        public GoldRateActivityBuilder verifiedBy(Long verifiedBy) {
            this.verifiedBy = verifiedBy;
            return this;
        }

        public GoldRateActivityBuilder verifiedByName(String verifiedByName) {
            this.verifiedByName = verifiedByName;
            return this;
        }

        public GoldRateActivityBuilder verifiedByEmpId(String verifiedByEmpId) {
            this.verifiedByEmpId = verifiedByEmpId;
            return this;
        }

        public GoldRateActivityBuilder status(String status) {
            this.status = status;
            return this;
        }

        public GoldRateActivityBuilder statusLookupId(Long statusLookupId) {
            this.statusLookupId = statusLookupId;
            return this;
        }

        public GoldRateActivityBuilder description(String description) {
            this.description = description;
            return this;
        }

        public GoldRateActivityBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public GoldRateActivityBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public GoldRateActivity build() {
            return new GoldRateActivity(id, goldRateId, activityDate, activityTime, module, recordId,
                    action, fieldName, oldValue, newValue, createdBy, createdByName, createdByEmpId,
                    modifiedBy, modifiedByName, modifiedByEmpId, verifiedBy, verifiedByName,
                    verifiedByEmpId, status, statusLookupId, description, ipAddress, createdAt);
        }
    }
}
