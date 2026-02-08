package com.wastina.trust.mapper;

import com.wastina.trust.dto.RoleRequestDto;
import com.wastina.trust.dto.RoleResponseDto;
import com.wastina.trust.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "id", ignore = true)
    Role toEntity(RoleRequestDto dto);

    RoleResponseDto toDto(Role entity);
}
