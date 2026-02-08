package com.wastina.trust.mapper;

import com.wastina.trust.dto.UserRequestDto;
import com.wastina.trust.dto.UserResponseDto;
import com.wastina.trust.entity.Role;
import com.wastina.trust.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true) // Handled by service
    @Mapping(target = "passwordHash", ignore = true) // Handled by service
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserRequestDto dto);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoles")
    UserResponseDto toDto(User entity);

    @Named("mapRoles")
    default Set<String> mapRoles(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }
}
