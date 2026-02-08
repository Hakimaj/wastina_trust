package com.wastina.trust.mapper;

import com.wastina.trust.dto.GuaranteeRequestDto;
import com.wastina.trust.dto.GuaranteeResponseDto;
import com.wastina.trust.entity.GuaranteeRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GuaranteeRequestMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "requester", ignore = true)
    GuaranteeRequest toEntity(GuaranteeRequestDto dto);

    @Mapping(target = "requesterId", source = "requester.id")
    GuaranteeResponseDto toDto(GuaranteeRequest entity);
}
