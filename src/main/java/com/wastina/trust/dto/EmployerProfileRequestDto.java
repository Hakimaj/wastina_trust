package com.wastina.trust.dto;

import lombok.Data;

@Data
public class EmployerProfileRequestDto {
    private String companyName;
    private String locationName; // Or locationId
    private String subscriptionTier;
}
