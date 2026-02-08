package com.wastina.trust.mapper;

import com.wastina.trust.dto.GuarantorProfileRequestDto;
import com.wastina.trust.dto.GuarantorProfileResponseDto;
import com.wastina.trust.entity.GuarantorProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GuarantorProfileMapper {
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "userId", ignore = true)
    GuarantorProfile toEntity(GuarantorProfileRequestDto dto);

    GuarantorProfileResponseDto toDto(GuarantorProfile entity);
}
