package com.wastina.trust.repository;

import com.wastina.trust.entity.EmploymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmploymentRecordRepository extends JpaRepository<EmploymentRecord, UUID> {
}
