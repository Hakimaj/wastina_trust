package com.wastina.trust.service.impl;

import com.wastina.trust.dto.PaymentRequestDto;
import com.wastina.trust.dto.PaymentResponseDto;
import com.wastina.trust.entity.EPaymentStatus;
import com.wastina.trust.entity.Payment;
import com.wastina.trust.entity.User;
import com.wastina.trust.mapper.PaymentMapper;
import com.wastina.trust.repository.PaymentRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        User payer = userRepository.findById(request.getPayerId())
                .orElseThrow(() -> new RuntimeException("Payer not found"));

        Payment payment = paymentMapper.toEntity(request);
        payment.setPayer(payer);
        payment.setStatus(EPaymentStatus.PENDING); // Default status
        payment.setTransactionId(UUID.randomUUID().toString()); // Mock transaction ID

        payment = paymentRepository.save(payment);
        return paymentMapper.toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByPayerId(UUID payerId) {
        // Assuming repository method exists or using generic find
        return paymentRepository.findAll().stream()
                .filter(p -> p.getPayer().getId().equals(payerId))
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        return paymentMapper.toDto(payment);
    }
}
