package com.wastina.trust.service.impl;

import com.wastina.trust.dto.RoleRequestDto;
import com.wastina.trust.dto.RoleResponseDto;
import com.wastina.trust.entity.Role;
import com.wastina.trust.mapper.RoleMapper;
import com.wastina.trust.repository.RoleRepository;
import com.wastina.trust.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponseDto createRole(RoleRequestDto request) {
        Role role = roleMapper.toEntity(request);
        role = roleRepository.save(role);
        return roleMapper.toDto(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponseDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponseDto getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
        return roleMapper.toDto(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new RuntimeException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }
}
