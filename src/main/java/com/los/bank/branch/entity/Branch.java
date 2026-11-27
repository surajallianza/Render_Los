package com.los.bank.branch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity mapping to existing bank branch table b001.
 */
@Entity
@Table(name = "b001")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "rbi_license_no", length = 50)
    private String rbiLicenseNo;

    @Column(name = "registration_no", length = 50)
    private String registrationNo;

    @Column(name = "incorporation_date")
    private LocalDate incorporationDate;

    @Column(name = "working_from")
    private LocalTime workingFrom;

    @Column(name = "working_to")
    private LocalTime workingTo;

    @Column(name = "address1", length = 255)
    private String address1;

    @Column(name = "address2", length = 255)
    private String address2;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "primary_contact_number", length = 20)
    private String primaryContactNumber;

    @Column(name = "secondary_contact_number", length = 20)
    private String secondaryContactNumber;

    @Column(name = "email_id", length = 150)
    private String emailId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
