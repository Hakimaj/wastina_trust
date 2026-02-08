package com.wastina.trust.repository;

import com.wastina.trust.entity.DigitalContract;
import com.wastina.trust.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DigitalContractRepository extends JpaRepository<DigitalContract, Long> {
    List<DigitalContract> findByWorker(User worker);

    List<DigitalContract> findByGuarantor(User guarantor);

    List<DigitalContract> findByEmployer(User employer);
}
