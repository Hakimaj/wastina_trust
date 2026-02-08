package com.wastina.trust.mapper;

import com.wastina.trust.dto.DigitalContractRequestDto;
import com.wastina.trust.dto.DigitalContractResponseDto;
import com.wastina.trust.entity.DigitalContract;
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
public class DigitalContractMapperImpl implements DigitalContractMapper {

    @Override
    public DigitalContract toEntity(DigitalContractRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        DigitalContract digitalContract = new DigitalContract();

        digitalContract.setLiabilityAmount( dto.getLiabilityAmount() );
        digitalContract.setTermsHash( dto.getTermsHash() );

        return digitalContract;
    }

    @Override
    public DigitalContractResponseDto toDto(DigitalContract entity) {
        if ( entity == null ) {
            return null;
        }

        DigitalContractResponseDto digitalContractResponseDto = new DigitalContractResponseDto();

        digitalContractResponseDto.setWorkerId( entityWorkerId( entity ) );
        digitalContractResponseDto.setGuarantorId( entityGuarantorId( entity ) );
        digitalContractResponseDto.setEmployerId( entityEmployerId( entity ) );
        digitalContractResponseDto.setId( entity.getId() );
        digitalContractResponseDto.setLiabilityAmount( entity.getLiabilityAmount() );
        digitalContractResponseDto.setTermsHash( entity.getTermsHash() );
        digitalContractResponseDto.setSignedDate( entity.getSignedDate() );

        return digitalContractResponseDto;
    }

    private UUID entityWorkerId(DigitalContract digitalContract) {
        if ( digitalContract == null ) {
            return null;
        }
        User worker = digitalContract.getWorker();
        if ( worker == null ) {
            return null;
        }
        UUID id = worker.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private UUID entityGuarantorId(DigitalContract digitalContract) {
        if ( digitalContract == null ) {
            return null;
        }
        User guarantor = digitalContract.getGuarantor();
        if ( guarantor == null ) {
            return null;
        }
        UUID id = guarantor.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private UUID entityEmployerId(DigitalContract digitalContract) {
        if ( digitalContract == null ) {
            return null;
        }
        User employer = digitalContract.getEmployer();
        if ( employer == null ) {
            return null;
        }
        UUID id = employer.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
