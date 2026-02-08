package com.wastina.trust.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class WorkerProfileResponseDto {
    private UUID userId;
    private String profession;
    private String location;
    private Integer trustScore;
    private boolean isAvailable;
    private boolean verificationBadge;
}
