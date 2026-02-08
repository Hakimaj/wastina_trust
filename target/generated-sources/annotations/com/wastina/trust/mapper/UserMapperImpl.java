package com.wastina.trust.mapper;

import com.wastina.trust.dto.UserRequestDto;
import com.wastina.trust.dto.UserResponseDto;
import com.wastina.trust.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-09T00:19:08+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        User user = new User();

        user.setPhoneNumber( dto.getPhoneNumber() );
        user.setEmail( dto.getEmail() );
        user.setFaydaId( dto.getFaydaId() );

        return user;
    }

    @Override
    public UserResponseDto toDto(User entity) {
        if ( entity == null ) {
            return null;
        }

        UserResponseDto userResponseDto = new UserResponseDto();

        userResponseDto.setRoles( mapRoles( entity.getRoles() ) );
        userResponseDto.setId( entity.getId() );
        userResponseDto.setPhoneNumber( entity.getPhoneNumber() );
        userResponseDto.setEmail( entity.getEmail() );
        userResponseDto.setFaydaId( entity.getFaydaId() );
        userResponseDto.setEnabled( entity.isEnabled() );
        userResponseDto.setCreatedAt( entity.getCreatedAt() );
        userResponseDto.setUpdatedAt( entity.getUpdatedAt() );

        return userResponseDto;
    }
}
