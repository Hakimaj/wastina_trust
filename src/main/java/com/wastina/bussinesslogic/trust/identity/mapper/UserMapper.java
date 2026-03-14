package com.wastina.trust.identity.mapper;

import com.wastina.trust.identity.dto.UserResponse;
import com.wastina.trust.identity.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final RoleMapper roleMapper;

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        if (user.getRoles() != null) {
            response.setRoles(user.getRoles().stream()
                    .map(roleMapper::toResponse)
                    .collect(Collectors.toSet()));
        }
        
        return response;
    }
}
