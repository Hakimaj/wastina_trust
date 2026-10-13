package com.wastina.trust.repository;

import com.wastina.trust.entity.GuaranteeRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuaranteeRequestRepository extends JpaRepository<GuaranteeRequest, Long> {

    List<GuaranteeRequest> findByRequester(com.wastina.trust.entity.User requester);

    List<GuaranteeRequest> findByGuarantorPhone(String guarantorPhone);

    @Query("select r from GuaranteeRequest r where r.requester.id = :requesterId order by r.createdAt desc")
    Page<GuaranteeRequest> findByRequesterId(@Param("requesterId") UUID requesterId, Pageable pageable);
}