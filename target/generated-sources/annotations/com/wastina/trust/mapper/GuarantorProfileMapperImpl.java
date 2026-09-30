package com.wastina.trust.mapper;

import com.wastina.trust.dto.GuarantorProfileRequestDto;
import com.wastina.trust.dto.GuarantorProfileResponseDto;
import com.wastina.trust.entity.GuarantorProfile;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-30T22:18:37+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class GuarantorProfileMapperImpl implements GuarantorProfileMapper {

    @Override
    public GuarantorProfile toEntity(GuarantorProfileRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        GuarantorProfile guarantorProfile = new GuarantorProfile();

        guarantorProfile.setEmployerName( dto.getEmployerName() );
        guarantorProfile.setFinancialLimit( dto.getFinancialLimit() );
        guarantorProfile.setKycStatus( dto.getKycStatus() );

        return guarantorProfile;
    }

    @Override
    public GuarantorProfileResponseDto toDto(GuarantorProfile entity) {
        if ( entity == null ) {
            return null;
        }

        GuarantorProfileResponseDto guarantorProfileResponseDto = new GuarantorProfileResponseDto();

        guarantorProfileResponseDto.setEmployerName( entity.getEmployerName() );
        guarantorProfileResponseDto.setFinancialLimit( entity.getFinancialLimit() );
        guarantorProfileResponseDto.setKycStatus( entity.getKycStatus() );
        guarantorProfileResponseDto.setUserId( entity.getUserId() );

        return guarantorProfileResponseDto;
    }
}
