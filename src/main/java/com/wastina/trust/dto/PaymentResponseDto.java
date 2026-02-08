package com.wastina.trust.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PaymentResponseDto {
    private Long id;
    private String transactionId;
    private UUID payerId;
    private BigDecimal amount;
    private String purpose;
    private String status;
    private LocalDateTime createdAt;
}
