package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.los.bank.lookup.dto.LookupValue;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviousEmploymentResponse {

    private Long id;
    private LookupValue employmentType;
    private String employerName;
    private String designation;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfJoining;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfRelieving;
}
