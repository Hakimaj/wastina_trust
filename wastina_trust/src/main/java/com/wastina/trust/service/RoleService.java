package com.wastina.trust.service;

import com.wastina.trust.dto.RoleRequestDto;
import com.wastina.trust.dto.RoleResponseDto;
import java.util.List;

public interface RoleService {
    RoleResponseDto createRole(RoleRequestDto request);

    List<RoleResponseDto> getAllRoles();

    RoleResponseDto getRoleById(Long id);

    void deleteRole(Long id);
}
