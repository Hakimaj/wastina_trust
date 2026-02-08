package com.wastina.trust.mapper;

import com.wastina.trust.dto.GuarantorProfileRequestDto;
import com.wastina.trust.dto.GuarantorProfileResponseDto;
import com.wastina.trust.entity.GuarantorProfile;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-09T00:19:08+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class GuarantorProfileMapperImpl implements GuarantorProfileMapper {

    @Override
    public GuarantorProfile toEntity(GuarantorProfileRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        GuarantorProfile guarantorProfile = new GuarantorProfile();

        guarantorProfile.setFinancialLimit( dto.getFinancialLimit() );
        guarantorProfile.setEmployerName( dto.getEmployerName() );
        guarantorProfile.setKycStatus( dto.getKycStatus() );

        return guarantorProfile;
    }

    @Override
    public GuarantorProfileResponseDto toDto(GuarantorProfile entity) {
        if ( entity == null ) {
            return null;
        }

        GuarantorProfileResponseDto guarantorProfileResponseDto = new GuarantorProfileResponseDto();

        guarantorProfileResponseDto.setUserId( entity.getUserId() );
        guarantorProfileResponseDto.setFinancialLimit( entity.getFinancialLimit() );
        guarantorProfileResponseDto.setEmployerName( entity.getEmployerName() );
        guarantorProfileResponseDto.setKycStatus( entity.getKycStatus() );

        return guarantorProfileResponseDto;
    }
}
