package com.wastina.trust.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class GuaranteeRequestDto {
    private UUID requesterId;
    private String guarantorPhone;
}
