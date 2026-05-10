package com.wastina.trust.mapper;

import com.wastina.trust.dto.WorkerProfileRequestDto;
import com.wastina.trust.dto.WorkerProfileResponseDto;
import com.wastina.trust.entity.Location;
import com.wastina.trust.entity.Profession;
import com.wastina.trust.entity.WorkerProfile;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-06T15:10:44+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class WorkerProfileMapperImpl implements WorkerProfileMapper {

    @Override
    public WorkerProfile toEntity(WorkerProfileRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        WorkerProfile workerProfile = new WorkerProfile();

        workerProfile.setAvailable( dto.isAvailable() );

        return workerProfile;
    }

    @Override
    public WorkerProfileResponseDto toDto(WorkerProfile entity) {
        if ( entity == null ) {
            return null;
        }

        WorkerProfileResponseDto workerProfileResponseDto = new WorkerProfileResponseDto();

        workerProfileResponseDto.setProfession( entityProfessionName( entity ) );
        workerProfileResponseDto.setLocation( entityLocationName( entity ) );
        workerProfileResponseDto.setAvailable( entity.isAvailable() );
        workerProfileResponseDto.setTrustScore( entity.getTrustScore() );
        workerProfileResponseDto.setUserId( entity.getUserId() );
        workerProfileResponseDto.setVerificationBadge( entity.isVerificationBadge() );

        return workerProfileResponseDto;
    }

    private String entityProfessionName(WorkerProfile workerProfile) {
        if ( workerProfile == null ) {
            return null;
        }
        Profession profession = workerProfile.getProfession();
        if ( profession == null ) {
            return null;
        }
        String name = profession.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String entityLocationName(WorkerProfile workerProfile) {
        if ( workerProfile == null ) {
            return null;
        }
        Location location = workerProfile.getLocation();
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
