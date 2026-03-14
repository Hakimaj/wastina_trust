package com.wastina.trust.identity.service;

import com.wastina.trust.identity.dto.UserRequest;
import com.wastina.trust.identity.dto.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserRequest request);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long id, UserRequest request);
    void deleteUser(Long id);
}
