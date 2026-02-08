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
    date = "2026-02-09T00:19:08+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
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
        paymentResponseDto.setId( entity.getId() );
        paymentResponseDto.setTransactionId( entity.getTransactionId() );
        paymentResponseDto.setAmount( entity.getAmount() );
        paymentResponseDto.setPurpose( entity.getPurpose() );
        if ( entity.getStatus() != null ) {
            paymentResponseDto.setStatus( entity.getStatus().name() );
        }
        paymentResponseDto.setCreatedAt( entity.getCreatedAt() );

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
