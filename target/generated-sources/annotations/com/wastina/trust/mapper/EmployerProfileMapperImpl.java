package com.wastina.trust.mapper;

import com.wastina.trust.dto.EmployerProfileRequestDto;
import com.wastina.trust.dto.EmployerProfileResponseDto;
import com.wastina.trust.entity.EmployerProfile;
import com.wastina.trust.entity.Location;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-06T15:10:44+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class EmployerProfileMapperImpl implements EmployerProfileMapper {

    @Override
    public EmployerProfile toEntity(EmployerProfileRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        EmployerProfile employerProfile = new EmployerProfile();

        employerProfile.setCompanyName( dto.getCompanyName() );
        employerProfile.setSubscriptionTier( dto.getSubscriptionTier() );

        return employerProfile;
    }

    @Override
    public EmployerProfileResponseDto toDto(EmployerProfile entity) {
        if ( entity == null ) {
            return null;
        }

        EmployerProfileResponseDto employerProfileResponseDto = new EmployerProfileResponseDto();

        employerProfileResponseDto.setLocation( entityLocationName( entity ) );
        employerProfileResponseDto.setCompanyName( entity.getCompanyName() );
        employerProfileResponseDto.setSubscriptionTier( entity.getSubscriptionTier() );
        employerProfileResponseDto.setTotalHires( entity.getTotalHires() );
        employerProfileResponseDto.setUserId( entity.getUserId() );

        return employerProfileResponseDto;
    }

    private String entityLocationName(EmployerProfile employerProfile) {
        if ( employerProfile == null ) {
            return null;
        }
        Location location = employerProfile.getLocation();
        if ( location == null ) {
            return null;
        }
        String name = location.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
