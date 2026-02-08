package com.wastina.trust.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DigitalContractResponseDto {
    private Long id;
    private UUID workerId;
    private UUID guarantorId;
    private UUID employerId;
    private BigDecimal liabilityAmount;
    private String termsHash;
    private LocalDateTime signedDate;
}
