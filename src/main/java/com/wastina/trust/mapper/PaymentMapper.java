package com.wastina.trust.mapper;

import com.wastina.trust.dto.PaymentRequestDto;
import com.wastina.trust.dto.PaymentResponseDto;
import com.wastina.trust.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "payer", ignore = true)
    Payment toEntity(PaymentRequestDto dto);

    @Mapping(target = "payerId", source = "payer.id")
    PaymentResponseDto toDto(Payment entity);
}
