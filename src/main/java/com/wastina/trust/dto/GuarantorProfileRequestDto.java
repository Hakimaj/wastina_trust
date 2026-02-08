package com.wastina.trust.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class GuarantorProfileRequestDto {
    private BigDecimal financialLimit;
    private String employerName;
    private String kycStatus; // Optional, might be system controlled
}
