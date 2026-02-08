package com.wastina.trust.mapper;

import com.wastina.trust.dto.RoleRequestDto;
import com.wastina.trust.dto.RoleResponseDto;
import com.wastina.trust.entity.ERole;
import com.wastina.trust.entity.Role;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-09T00:19:09+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class RoleMapperImpl implements RoleMapper {

    @Override
    public Role toEntity(RoleRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        Role role = new Role();

        if ( dto.getName() != null ) {
            role.setName( Enum.valueOf( ERole.class, dto.getName() ) );
        }
        role.setDescription( dto.getDescription() );

        return role;
    }

    @Override
    public RoleResponseDto toDto(Role entity) {
        if ( entity == null ) {
            return null;
        }

        RoleResponseDto roleResponseDto = new RoleResponseDto();

        roleResponseDto.setId( entity.getId() );
        if ( entity.getName() != null ) {
            roleResponseDto.setName( entity.getName().name() );
        }
        roleResponseDto.setDescription( entity.getDescription() );

        return roleResponseDto;
    }
}
