package com.wastina.trust.controller;

import com.wastina.trust.dto.RoleRequestDto;
import com.wastina.trust.dto.RoleResponseDto;
import com.wastina.trust.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_role:manage')")
    public ResponseEntity<RoleResponseDto> createRole(@RequestBody RoleRequestDto request) {
        RoleResponseDto response = roleService.createRole(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_role:read')")
    public ResponseEntity<List<RoleResponseDto>> getAllRoles() {
        List<RoleResponseDto> response = roleService.getAllRoles();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_role:read')")
    public ResponseEntity<RoleResponseDto> getRoleById(@PathVariable Long id) {
        RoleResponseDto response = roleService.getRoleById(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_role:manage')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
