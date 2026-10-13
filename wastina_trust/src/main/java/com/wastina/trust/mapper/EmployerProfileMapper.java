package com.wastina.trust.mapper;

import com.wastina.trust.dto.EmployerProfileRequestDto;
import com.wastina.trust.dto.EmployerProfileResponseDto;
import com.wastina.trust.entity.EmployerProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployerProfileMapper {
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "totalHires", ignore = true)
    @Mapping(target = "location", ignore = true) // Handled by service
    EmployerProfile toEntity(EmployerProfileRequestDto dto);

    @Mapping(target = "location", source = "location.name")
    EmployerProfileResponseDto toDto(EmployerProfile entity);
}
