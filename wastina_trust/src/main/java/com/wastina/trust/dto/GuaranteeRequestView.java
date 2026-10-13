package com.wastina.trust.dto;

import com.wastina.trust.entity.GuaranteeRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A guarantee request (the SMS-link flow).
 *
 * <p>Responses report the requester as an opaque identifier. {@code guarantorPhone} is retained
 * because it is the address the request is actually sent to; it is exposed only to the requester.
 */
@Schema(name = "GuaranteeRequest")
public record GuaranteeRequestView(
        @Schema(description = "Database identifier; treat as opaque")
        Long id,

        @Schema(description = "Identifier of the requester")
        UUID requesterId,

        @Schema(description = "Phone number the request was sent to")
        String guarantorPhone,

        @Schema(description = "Request state")
        String status,

        @Schema(description = "When the request was created")
        LocalDateTime createdAt,

        @Schema(description = "When the request stops being valid")
        LocalDateTime expiresAt) {

    public static GuaranteeRequestView from(GuaranteeRequest request) {
        return new GuaranteeRequestView(
                request.getId(),
                request.getRequester() == null ? null : request.getRequester().getId(),
                request.getGuarantorPhone(),
                request.getStatus() == null ? null : request.getStatus().name(),
                request.getCreatedAt(),
                request.getExpiresAt());
    }
}