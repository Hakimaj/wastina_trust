package com.wastina.trust.identity.mapper;

import com.wastina.trust.identity.dto.PermissionResponse;
import com.wastina.trust.identity.dto.RoleResponse;
import com.wastina.trust.identity.dto.UserResponse;
import com.wastina.trust.identity.entity.Permission;
import com.wastina.trust.identity.entity.Role;
import com.wastina.trust.identity.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

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
                    .map(this::toRoleResponse)
                    .collect(Collectors.toSet()));
        }
        
        return response;
    }

    public RoleResponse toRoleResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        
        if (role.getPermissions() != null) {
            response.setPermissions(role.getPermissions().stream()
                    .map(this::toPermissionResponse)
                    .collect(Collectors.toSet()));
        }
        
        return response;
    }

    public PermissionResponse toPermissionResponse(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setName(permission.getName());
        response.setDescription(permission.getDescription());
        return response;
    }
}
