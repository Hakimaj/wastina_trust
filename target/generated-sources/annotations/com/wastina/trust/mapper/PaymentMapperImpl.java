package com.wastina.trust.mapper;

import com.wastina.trust.dto.PaymentRequestDto;
import com.wastina.trust.dto.PaymentResponseDto;
import com.wastina.trust.entity.Payment;
import com.wastina.trust.entity.User;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-06T15:10:44+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class PaymentMapperImpl implements PaymentMapper {

    @Override
    public Payment toEntity(PaymentRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        Payment payment = new Payment();

        payment.setAmount( dto.getAmount() );
        payment.setPurpose( dto.getPurpose() );

        return payment;
    }

    @Override
    public PaymentResponseDto toDto(Payment entity) {
        if ( entity == null ) {
            return null;
        }

        PaymentResponseDto paymentResponseDto = new PaymentResponseDto();

        paymentResponseDto.setPayerId( entityPayerId( entity ) );
        paymentResponseDto.setAmount( entity.getAmount() );
        paymentResponseDto.setCreatedAt( entity.getCreatedAt() );
        paymentResponseDto.setId( entity.getId() );
        paymentResponseDto.setPurpose( entity.getPurpose() );
        if ( entity.getStatus() != null ) {
            paymentResponseDto.setStatus( entity.getStatus().name() );
        }
        paymentResponseDto.setTransactionId( entity.getTransactionId() );

        return paymentResponseDto;
    }

    private UUID entityPayerId(Payment payment) {
        if ( payment == null ) {
            return null;
        }
        User payer = payment.getPayer();
        if ( payer == null ) {
            return null;
        }
        UUID id = payer.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
