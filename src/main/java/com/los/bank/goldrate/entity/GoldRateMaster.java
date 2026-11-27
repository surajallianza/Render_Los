package com.los.bank.goldrate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity mapping table g010 (Gold Rate Master).
 * Represents daily benchmark Gold Rates per gram by purity.
 */
@Entity
@Table(name = "g010")
public class GoldRateMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false, unique = true, length = 50)
    private String recordId;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "effective_time")
    private LocalTime effectiveTime;

    @Column(name = "rate_24k", nullable = false, precision = 15, scale = 2)
    private BigDecimal rate24k;

    @Column(name = "rate_22k", nullable = false, precision = 15, scale = 2)
    private BigDecimal rate22k;

    @Column(name = "rate_18k", precision = 15, scale = 2)
    private BigDecimal rate18k;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "status_lookup_id")
    private Long statusLookupId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_name", length = 100)
    private String createdByName;

    @Column(name = "created_by_emp_id", length = 50)
    private String createdByEmpId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private Long modifiedBy;

    @Column(name = "modified_by_name", length = 100)
    private String modifiedByName;

    @Column(name = "modified_by_emp_id", length = 50)
    private String modifiedByEmpId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_by_name", length = 100)
    private String verifiedByName;

    @Column(name = "verified_by_emp_id", length = 50)
    private String verifiedByEmpId;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    public GoldRateMaster() {
    }

    public GoldRateMaster(Long id, String recordId, LocalDate effectiveDate, LocalTime effectiveTime,
                          BigDecimal rate24k, BigDecimal rate22k, BigDecimal rate18k,
                          String currency, String unit, String status, Long statusLookupId,
                          String description, Long createdBy, String createdByName,
                          String createdByEmpId, LocalDateTime createdAt, Long modifiedBy,
                          String modifiedByName, String modifiedByEmpId, LocalDateTime updatedAt,
                          Long verifiedBy, String verifiedByName, String verifiedByEmpId,
                          LocalDateTime verifiedAt) {
        this.id = id;
        this.recordId = recordId;
        this.effectiveDate = effectiveDate;
        this.effectiveTime = effectiveTime;
        this.rate24k = rate24k;
        this.rate22k = rate22k;
        this.rate18k = rate18k;
        this.currency = currency;
        this.unit = unit;
        this.status = status;
        this.statusLookupId = statusLookupId;
        this.description = description;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdByEmpId = createdByEmpId;
        this.createdAt = createdAt;
        this.modifiedBy = modifiedBy;
        this.modifiedByName = modifiedByName;
        this.modifiedByEmpId = modifiedByEmpId;
        this.updatedAt = updatedAt;
        this.verifiedBy = verifiedBy;
        this.verifiedByName = verifiedByName;
        this.verifiedByEmpId = verifiedByEmpId;
        this.verifiedAt = verifiedAt;
    }

    public static GoldRateMasterBuilder builder() {
        return new GoldRateMasterBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalTime getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(LocalTime effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public BigDecimal getRate24k() {
        return rate24k;
    }

    public void setRate24k(BigDecimal rate24k) {
        this.rate24k = rate24k;
    }

    public BigDecimal getRate22k() {
        return rate22k;
    }

    public void setRate22k(BigDecimal rate22k) {
        this.rate22k = rate22k;
    }

    public BigDecimal getRate18k() {
        return rate18k;
    }

    public void setRate18k(BigDecimal rate18k) {
        this.rate18k = rate18k;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public static class GoldRateMasterBuilder {
        private Long builderId;
        private String recordId;
        private LocalDate effectiveDate;
        private LocalTime effectiveTime;
        private BigDecimal rate24k;
        private BigDecimal rate22k;
        private BigDecimal rate18k;
        private String currency;
        private String unit;
        private String status;
        private Long statusLookupId;
        private String description;
        private Long createdBy;
        private String createdByName;
        private String createdByEmpId;
        private LocalDateTime createdAt;
        private Long modifiedBy;
        private String modifiedByName;
        private String modifiedByEmpId;
        private LocalDateTime updatedAt;
        private Long verifiedBy;
        private String verifiedByName;
        private String verifiedByEmpId;
        private LocalDateTime verifiedAt;

        GoldRateMasterBuilder() {
        }

        public GoldRateMasterBuilder id(Long id) {
            this.builderId = id;
            return this;
        }

        public GoldRateMasterBuilder recordId(String recordId) {
            this.recordId = recordId;
            return this;
        }

        public GoldRateMasterBuilder effectiveDate(LocalDate effectiveDate) {
            this.effectiveDate = effectiveDate;
            return this;
        }

        public GoldRateMasterBuilder effectiveTime(LocalTime effectiveTime) {
            this.effectiveTime = effectiveTime;
            return this;
        }

        public GoldRateMasterBuilder rate24k(BigDecimal rate24k) {
            this.rate24k = rate24k;
            return this;
        }

        public GoldRateMasterBuilder rate22k(BigDecimal rate22k) {
            this.rate22k = rate22k;
            return this;
        }

        public GoldRateMasterBuilder rate18k(BigDecimal rate18k) {
            this.rate18k = rate18k;
            return this;
        }

        public GoldRateMasterBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public GoldRateMasterBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public GoldRateMasterBuilder status(String status) {
            this.status = status;
            return this;
        }

        public GoldRateMasterBuilder statusLookupId(Long statusLookupId) {
            this.statusLookupId = statusLookupId;
            return this;
        }

        public GoldRateMasterBuilder description(String description) {
            this.description = description;
            return this;
        }

        public GoldRateMasterBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public GoldRateMasterBuilder createdByName(String createdByName) {
            this.createdByName = createdByName;
            return this;
        }

        public GoldRateMasterBuilder createdByEmpId(String createdByEmpId) {
            this.createdByEmpId = createdByEmpId;
            return this;
        }

        public GoldRateMasterBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public GoldRateMasterBuilder modifiedBy(Long modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        public GoldRateMasterBuilder modifiedByName(String modifiedByName) {
            this.modifiedByName = modifiedByName;
            return this;
        }

        public GoldRateMasterBuilder modifiedByEmpId(String modifiedByEmpId) {
            this.modifiedByEmpId = modifiedByEmpId;
            return this;
        }

        public GoldRateMasterBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public GoldRateMasterBuilder verifiedBy(Long verifiedBy) {
            this.verifiedBy = verifiedBy;
            return this;
        }

        public GoldRateMasterBuilder verifiedByName(String verifiedByName) {
            this.verifiedByName = verifiedByName;
            return this;
        }

        public GoldRateMasterBuilder verifiedByEmpId(String verifiedByEmpId) {
            this.verifiedByEmpId = verifiedByEmpId;
            return this;
        }

        public GoldRateMasterBuilder verifiedAt(LocalDateTime verifiedAt) {
            this.verifiedAt = verifiedAt;
            return this;
        }

        public GoldRateMaster build() {
            return new GoldRateMaster(this.builderId, this.recordId, this.effectiveDate, this.effectiveTime,
                    this.rate24k, this.rate22k, this.rate18k, this.currency, this.unit, this.status,
                    this.statusLookupId, this.description, this.createdBy, this.createdByName,
                    this.createdByEmpId, this.createdAt, this.modifiedBy, this.modifiedByName,
                    this.modifiedByEmpId, this.updatedAt, this.verifiedBy, this.verifiedByName,
                    this.verifiedByEmpId, this.verifiedAt);
        }
    }
}
