package com.wastina.trust.service;

import com.wastina.trust.dto.DigitalContractRequestDto;
import com.wastina.trust.dto.DigitalContractResponseDto;
import java.util.List;
import java.util.UUID;

public interface DigitalContractService {
    DigitalContractResponseDto createContract(DigitalContractRequestDto request);

    List<DigitalContractResponseDto> getAllContracts();

    DigitalContractResponseDto getContractById(Long id);

    List<DigitalContractResponseDto> getContractsByWorkerId(UUID workerId);
}
