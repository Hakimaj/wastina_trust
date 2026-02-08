package com.wastina.trust.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
public class UserResponseDto {
    private UUID id;
    private String phoneNumber;
    private String email;
    private String faydaId;
    private boolean isEnabled;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
