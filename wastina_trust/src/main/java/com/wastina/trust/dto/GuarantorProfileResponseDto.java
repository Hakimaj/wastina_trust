package com.wastina.trust.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class GuarantorProfileResponseDto {
    private UUID userId;
    private BigDecimal financialLimit;
    private String employerName;
    private String kycStatus;
}
