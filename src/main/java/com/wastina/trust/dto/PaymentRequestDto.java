package com.wastina.trust.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequestDto {
    private UUID payerId;
    private BigDecimal amount;
    private String purpose;
}
