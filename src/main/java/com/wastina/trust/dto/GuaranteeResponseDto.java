package com.wastina.trust.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GuaranteeResponseDto {
    private Long id;
    private UUID requesterId;
    private String guarantorPhone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
