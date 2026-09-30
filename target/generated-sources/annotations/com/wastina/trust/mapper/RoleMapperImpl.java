package com.wastina.trust.mapper;

import com.wastina.trust.dto.RoleRequestDto;
import com.wastina.trust.dto.RoleResponseDto;
import com.wastina.trust.entity.ERole;
import com.wastina.trust.entity.Role;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-30T20:28:07+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class RoleMapperImpl implements RoleMapper {

    @Override
    public Role toEntity(RoleRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        Role role = new Role();

        role.setDescription( dto.getDescription() );
        if ( dto.getName() != null ) {
            role.setName( Enum.valueOf( ERole.class, dto.getName() ) );
        }

        return role;
    }

    @Override
    public RoleResponseDto toDto(Role entity) {
        if ( entity == null ) {
            return null;
        }

        RoleResponseDto roleResponseDto = new RoleResponseDto();

        roleResponseDto.setDescription( entity.getDescription() );
        roleResponseDto.setId( entity.getId() );
        if ( entity.getName() != null ) {
            roleResponseDto.setName( entity.getName().name() );
        }

        return roleResponseDto;
    }
}
