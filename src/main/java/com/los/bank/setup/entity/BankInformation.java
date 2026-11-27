package com.los.bank.setup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_information")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "bank_code", nullable = false, unique = true, length = 50)
    private String bankCode;

    @Column(name = "bank_email", nullable = false, length = 150)
    private String bankEmail;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    // Registration Details
    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "bank_type", length = 50)
    private String bankType;

    @Column(name = "pan", length = 20)
    private String pan;

    @Column(name = "gst_no", length = 30)
    private String gstNo;

    @Column(name = "license_no", length = 50)
    private String licenseNo;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    // Status and Regulatory Information
    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "regulatory_status", length = 30)
    private String regulatoryStatus;

    // Banking Membership Details
    @Column(name = "direct_clg_member")
    private Boolean directClgMember;

    @Column(name = "direct_member_iftas")
    private Boolean directMemberIftas;

    // MICR and IFSC Details
    @Column(name = "micr1", length = 20)
    private String micr1;

    @Column(name = "micr2", length = 20)
    private String micr2;

    @Column(name = "micr3", length = 20)
    private String micr3;

    @Column(name = "micr_number", length = 30)
    private String micrNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    // Branch Information
    @Column(name = "no_of_branches")
    private Integer noOfBranches;

    // Sponsor Bank Details
    @Column(name = "sponsor_bank_clg", length = 150)
    private String sponsorBankClg;

    @Column(name = "sponsor_bank_iftas", length = 150)
    private String sponsorBankIftas;

    // Registered Office Address
    @Column(name = "address_type", length = 50)
    private String addressType;

    @Column(name = "unit_gala", length = 150)
    private String unitGala;

    @Column(name = "street_road", length = 200)
    private String streetRoad;

    @Column(name = "landmark", length = 150)
    private String landmark;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
        if (regulatoryStatus == null) {
            regulatoryStatus = "ACTIVE";
        }
        if (directClgMember == null) {
            directClgMember = false;
        }
        if (directMemberIftas == null) {
            directMemberIftas = false;
        }
        if (country == null) {
            country = "India";
        }
        if (addressType == null) {
            addressType = "Registered Office";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
