package com.wastina.trust.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class WorkerProfileRequestDto {
    private String professionName; // Or professionId if client sends ID
    private String locationName; // Or locationId
    private boolean isAvailable;
}
