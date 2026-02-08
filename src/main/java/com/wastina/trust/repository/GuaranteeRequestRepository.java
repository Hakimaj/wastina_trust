package com.wastina.trust.repository;

import com.wastina.trust.entity.GuaranteeRequest;
import com.wastina.trust.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuaranteeRequestRepository extends JpaRepository<GuaranteeRequest, Long> {
    List<GuaranteeRequest> findByRequester(User requester);

    List<GuaranteeRequest> findByGuarantorPhone(String guarantorPhone);
}
