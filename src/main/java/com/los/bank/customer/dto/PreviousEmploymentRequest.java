package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviousEmploymentRequest {

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String employmentType;

    private String employerName;
    private String designation;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfJoining;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfRelieving;
}
