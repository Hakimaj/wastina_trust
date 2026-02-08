package com.wastina.trust.dto;

import lombok.Data;
import java.util.Set;
import java.util.UUID;

@Data
public class UserRequestDto {
    private String phoneNumber;
    private String password; // Raw password for creation/update
    private String email;
    private String faydaId;
    private Set<Long> roleIds;
}
