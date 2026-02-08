package com.wastina.trust.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class DigitalContractRequestDto {
    private UUID workerId;
    private UUID guarantorId;
    private UUID employerId; // Optional
    private BigDecimal liabilityAmount;
    private String termsHash;
}
