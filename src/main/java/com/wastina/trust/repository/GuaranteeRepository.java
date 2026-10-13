package com.wastina.trust.repository;

import com.wastina.trust.entity.Guarantee;
import com.wastina.trust.entity.GuaranteeStatus;
import com.wastina.trust.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuaranteeRepository extends JpaRepository<Guarantee, Long> {

    Optional<Guarantee> findByPublicReference(String publicReference);

    List<Guarantee> findAllByOrderByCreatedAtDesc();

    Page<Guarantee> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Guarantees where the user is the principal (the worker being backed). */
    @Query("select g from Guarantee g where g.principal.id = :userId order by g.createdAt desc")
    Page<Guarantee> findByPrincipal(@Param("userId") UUID userId, Pageable pageable);

    /** Guarantees where the user is the guarantor (the person taking on the liability). */
    @Query("select g from Guarantee g where g.guarantor.id = :userId order by g.createdAt desc")
    Page<Guarantee> findByGuarantor(@Param("userId") UUID userId, Pageable pageable);

    List<Guarantee> findAllByGuarantorAndStatusIn(User guarantor, java.util.Collection<GuaranteeStatus> statuses);
}