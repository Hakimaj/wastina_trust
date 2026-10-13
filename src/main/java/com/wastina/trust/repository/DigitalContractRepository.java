package com.wastina.trust.repository;

import com.wastina.trust.entity.DigitalContract;
import com.wastina.trust.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DigitalContractRepository extends JpaRepository<DigitalContract, Long> {

    List<DigitalContract> findByWorker(User worker);

    List<DigitalContract> findByGuarantor(User guarantor);

    List<DigitalContract> findByEmployer(User employer);

    @Query("select c from DigitalContract c where c.worker.id = :workerId order by c.signedDate desc")
    Page<DigitalContract> findByWorkerId(@Param("workerId") UUID workerId, Pageable pageable);

    /**
     * Contracts the user participates in on either side. There is deliberately no unrestricted
     * {@code findAll} exposure on the API: the previous
     * {@code GET /api/contracts} returned every contract in the system to any authenticated caller.
     */
    @Query("select c from DigitalContract c where c.worker.id = :userId "
            + "or c.guarantor.id = :userId or c.employer.id = :userId order by c.signedDate desc")
    Page<DigitalContract> findInvolvingUser(@Param("userId") UUID userId, Pageable pageable);

    boolean existsByWorkerIdAndGuarantorIdAndEmployerId(UUID workerId, UUID guarantorId, UUID employerId);
}