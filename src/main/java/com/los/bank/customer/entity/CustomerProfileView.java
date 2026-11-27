package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only entity mapped to view v_customer_profile for customer list & search.
 */
@Entity
@Table(name = "v_customer_profile")
@Immutable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileView {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_code")
    private String customerCode;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "lead_id")
    private String leadId;

    @Column(name = "title")
    private String title;

    @Column(name = "gender")
    private String gender;

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "customer_type")
    private String customerType;

    @Column(name = "kyc_status")
    private String kycStatus;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "age")
    private Integer age;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "alternate_contact")
    private String alternateContact;

    @Column(name = "residential_status")
    private String residentialStatus;

    @Column(name = "residential_type")
    private String residentialType;

    @Column(name = "flat_building_name")
    private String flatBuildingName;

    @Column(name = "street_road")
    private String streetRoad;

    @Column(name = "landmark")
    private String landmark;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "employer_type")
    private String employerType;

    @Column(name = "designation")
    private String designation;

    @Column(name = "employer_name")
    private String employerName;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "qualification")
    private String qualification;

    @Column(name = "employer_email")
    private String employerEmail;

    @Column(name = "listed_status")
    private String listedStatus;

    @Column(name = "years_of_service")
    private BigDecimal yearsOfService;

    @Column(name = "employment_location")
    private String employmentLocation;

    @Column(name = "date_of_retirement")
    private LocalDate dateOfRetirement;

    @Column(name = "annual_income")
    private BigDecimal annualIncome;

    @Column(name = "other_source_income")
    private BigDecimal otherSourceIncome;

    @Column(name = "total_gross_income")
    private BigDecimal totalGrossIncome;

    @Column(name = "total_net_income")
    private BigDecimal totalNetIncome;

    @Column(name = "income_tax")
    private BigDecimal incomeTax;

    @Column(name = "income_tax_filer_status")
    private String incomeTaxFilerStatus;

    @Column(name = "risk_category")
    private String riskCategory;

    @Column(name = "onboarding_status")
    private String onboardingStatus;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
