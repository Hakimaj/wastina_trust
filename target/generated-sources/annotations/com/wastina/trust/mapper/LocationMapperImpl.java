package com.wastina.trust.mapper;

import com.wastina.trust.dto.LocationRequestDto;
import com.wastina.trust.dto.LocationResponseDto;
import com.wastina.trust.entity.Location;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-30T20:28:07+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class LocationMapperImpl implements LocationMapper {

    @Override
    public Location toEntity(LocationRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        Location location = new Location();

        location.setName( dto.getName() );

        return location;
    }

    @Override
    public LocationResponseDto toDto(Location entity) {
        if ( entity == null ) {
            return null;
        }

        LocationResponseDto locationResponseDto = new LocationResponseDto();

        locationResponseDto.setId( entity.getId() );
        locationResponseDto.setName( entity.getName() );

        return locationResponseDto;
    }
}
