package com.wastina.trust.repository;

import com.wastina.trust.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppealRepository extends JpaRepository<Appeal, UUID> {
}
