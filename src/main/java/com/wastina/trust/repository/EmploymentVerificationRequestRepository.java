package com.wastina.trust.repository;

import com.wastina.trust.entity.EmploymentVerificationRequest;
import com.wastina.trust.entity.EmploymentVerificationRequestStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentVerificationRequestRepository
        extends JpaRepository<EmploymentVerificationRequest, UUID> {

    List<EmploymentVerificationRequest> findAllByRequesterUser_IdOrderByRequestedAtDesc(UUID requesterUserId);

    List<EmploymentVerificationRequest> findAllByTargetOrganization_IdOrderByRequestedAtDesc(UUID organizationId);

    /**
     * Reports whether a record already has a verification request in one of the given open states.
     * Used to refuse duplicate open requests instead of creating an unbounded stream of them.
     */
    boolean existsByEmploymentRecord_IdAndStatusIn(UUID employmentRecordId,
                                                   Collection<EmploymentVerificationRequestStatus> statuses);
}