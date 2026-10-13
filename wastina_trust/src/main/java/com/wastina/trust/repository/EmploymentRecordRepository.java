package com.wastina.trust.repository;

import com.wastina.trust.entity.EmploymentRecord;
import com.wastina.trust.entity.EmploymentVerificationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentRecordRepository extends JpaRepository<EmploymentRecord, UUID> {

    List<EmploymentRecord> findAllByWorker_IdOrderByStartDateDesc(UUID workerId);

    Optional<EmploymentRecord> findByIdAndWorker_Id(UUID id, UUID workerId);

    /**
     * Paged listing of an organization's employment records. Added so an employer's record view can
     * grow without loading the whole history.
     */
    Page<EmploymentRecord> findByOrganization_Id(UUID organizationId, Pageable pageable);

    List<EmploymentRecord> findAllByOrganization_IdAndVerificationStatus(
            UUID organizationId, EmploymentVerificationStatus verificationStatus);
}