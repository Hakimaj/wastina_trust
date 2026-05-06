package com.wastina.trust.mapper;

import com.wastina.trust.dto.UserRequestDto;
import com.wastina.trust.dto.UserResponseDto;
import com.wastina.trust.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-06T14:54:06+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        User user = new User();

        user.setEmail( dto.getEmail() );
        user.setFaydaId( dto.getFaydaId() );
        user.setPhoneNumber( dto.getPhoneNumber() );

        return user;
    }

    @Override
    public UserResponseDto toDto(User entity) {
        if ( entity == null ) {
            return null;
        }

        UserResponseDto userResponseDto = new UserResponseDto();

        userResponseDto.setRoles( mapRoles( entity.getRoles() ) );
        userResponseDto.setCreatedAt( entity.getCreatedAt() );
        userResponseDto.setEmail( entity.getEmail() );
        userResponseDto.setEnabled( entity.isEnabled() );
        userResponseDto.setFaydaId( entity.getFaydaId() );
        userResponseDto.setId( entity.getId() );
        userResponseDto.setPhoneNumber( entity.getPhoneNumber() );
        userResponseDto.setUpdatedAt( entity.getUpdatedAt() );

        return userResponseDto;
    }
}
