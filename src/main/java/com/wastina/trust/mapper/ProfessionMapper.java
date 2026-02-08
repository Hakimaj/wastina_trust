package com.wastina.trust.mapper;

import com.wastina.trust.dto.ProfessionRequestDto;
import com.wastina.trust.dto.ProfessionResponseDto;
import com.wastina.trust.entity.Profession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfessionMapper {
    @Mapping(target = "id", ignore = true)
    Profession toEntity(ProfessionRequestDto dto);

    ProfessionResponseDto toDto(Profession entity);
}
