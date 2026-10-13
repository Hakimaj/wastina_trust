package com.wastina.trust.service;

import com.wastina.trust.dto.DigitalContractRequestDto;
import com.wastina.trust.dto.DigitalContractResponseDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Digital contract access.
 *
 * <p>All read operations are scoped to a user, and creation derives the worker from the
 * authenticated caller. There is no unrestricted "list everything" operation.
 */
public interface DigitalContractService {

    DigitalContractResponseDto createContractForWorker(UUID workerId, DigitalContractRequestDto request);

    Page<DigitalContractResponseDto> listContractsInvolvingUser(UUID userId, Pageable pageable);

    /** Returns {@code 404} when the contract does not exist or the user is not a party to it. */
    DigitalContractResponseDto getContractForUser(Long id, UUID userId);
}