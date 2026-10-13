package com.wastina.trust.repository;

import com.wastina.trust.entity.Payment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPayer(com.wastina.trust.entity.User payer);

    @Query("select p from Payment p where p.payer.id = :payerId order by p.createdAt desc")
    Page<Payment> findByPayerId(@Param("payerId") UUID payerId, Pageable pageable);
}