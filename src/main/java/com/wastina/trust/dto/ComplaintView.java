package com.wastina.trust.dto;

import com.wastina.trust.entity.Complaint;
import com.wastina.trust.entity.ComplaintCategory;
import com.wastina.trust.entity.ComplaintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * A complaint.
 *
 * <p>Parties are opaque identifiers. The previous response serialized the nested {@code User}
 * entities, which exposed each party's email address and phone number to anyone who could read the
 * complaint.
 */
@Schema(name = "Complaint")
public record ComplaintView(
        @Schema(description = "Public reference, e.g. WST-CMP-1A2B3C4D")
        String publicReference,

        @Schema(description = "Database identifier; treat as opaque")
        UUID id,

        @Schema(description = "Identifier of the complainant")
        UUID complainantId,

        @Schema(description = "Identifier of the subject of the complaint")
        UUID subjectId,

        @Schema(description = "Complaint category")
        ComplaintCategory category,

        @Schema(description = "Complaint detail")
        String description,

        @Schema(description = "Relationship between the parties")
        String relationshipType,

        @Schema(description = "Lifecycle state")
        ComplaintStatus status,

        @Schema(description = "Assigned severity, when set")
        String severity,

        @Schema(description = "When the complaint was submitted")
        Instant submittedAt,

        @Schema(description = "When the complaint was closed, if it has been")
        Instant closedAt) {

    public static ComplaintView from(Complaint complaint) {
        return new ComplaintView(
                complaint.getPublicReference(),
                complaint.getId(),
                complaint.getComplainant() == null ? null : complaint.getComplainant().getId(),
                complaint.getSubject() == null ? null : complaint.getSubject().getId(),
                complaint.getCategory(),
                complaint.getDescription(),
                complaint.getRelationshipType(),
                complaint.getStatus(),
                complaint.getSeverity() == null ? null : complaint.getSeverity().name(),
                complaint.getSubmittedAt(),
                complaint.getClosedAt());
    }
}