package com.wastina.trust.service;

import com.wastina.trust.dto.PaymentRequestDto;
import com.wastina.trust.dto.PaymentResponseDto;
import java.util.List;
import java.util.UUID;

public interface PaymentService {
    PaymentResponseDto createPayment(PaymentRequestDto request);

    List<PaymentResponseDto> getPaymentsByPayerId(UUID payerId);

    PaymentResponseDto getPaymentById(Long id);
}
