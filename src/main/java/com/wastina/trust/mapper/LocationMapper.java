package com.wastina.trust.mapper;

import com.wastina.trust.dto.LocationRequestDto;
import com.wastina.trust.dto.LocationResponseDto;
import com.wastina.trust.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    @Mapping(target = "id", ignore = true)
    Location toEntity(LocationRequestDto dto);

    LocationResponseDto toDto(Location entity);
}
