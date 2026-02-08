package com.wastina.trust.service;

import com.wastina.trust.dto.UserRequestDto;
import com.wastina.trust.dto.UserResponseDto;
import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponseDto createUser(UserRequestDto request);

    List<UserResponseDto> getAllUsers();

    UserResponseDto getUserById(UUID id);

    UserResponseDto updateUser(UUID id, UserRequestDto request);

    void deleteUser(UUID id);

    UserResponseDto addRoleToUser(UUID userId, Long roleId);
}
