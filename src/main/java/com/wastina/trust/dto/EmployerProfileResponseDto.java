package com.wastina.trust.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class EmployerProfileResponseDto {
    private UUID userId;
    private String companyName;
    private String location;
    private String subscriptionTier;
    private Integer totalHires;
}
