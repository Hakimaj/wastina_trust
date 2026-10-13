package com.wastina.trust.mapper;

import com.wastina.trust.dto.WorkerProfileRequestDto;
import com.wastina.trust.dto.WorkerProfileResponseDto;
import com.wastina.trust.entity.WorkerProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkerProfileMapper {
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "trustScore", ignore = true)
    @Mapping(target = "verificationBadge", ignore = true)
    @Mapping(target = "profession", ignore = true) // Handled by service
    @Mapping(target = "location", ignore = true) // Handled by service
    WorkerProfile toEntity(WorkerProfileRequestDto dto);

    @Mapping(target = "profession", source = "profession.name")
    @Mapping(target = "location", source = "location.name")
    WorkerProfileResponseDto toDto(WorkerProfile entity);
}
