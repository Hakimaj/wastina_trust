package com.wastina.trust.service.impl;

import com.wastina.trust.dto.DigitalContractRequestDto;
import com.wastina.trust.dto.DigitalContractResponseDto;
import com.wastina.trust.entity.DigitalContract;
import com.wastina.trust.entity.User;
import com.wastina.trust.mapper.DigitalContractMapper;
import com.wastina.trust.repository.DigitalContractRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.service.DigitalContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DigitalContractServiceImpl implements DigitalContractService {

    private final DigitalContractRepository digitalContractRepository;
    private final UserRepository userRepository;
    private final DigitalContractMapper digitalContractMapper;

    @Override
    @Transactional
    public DigitalContractResponseDto createContract(DigitalContractRequestDto request) {
        User worker = userRepository.findById(request.getWorkerId())
                .orElseThrow(() -> new RuntimeException("Worker not found"));
        User guarantor = userRepository.findById(request.getGuarantorId())
                .orElseThrow(() -> new RuntimeException("Guarantor not found"));

        DigitalContract contract = digitalContractMapper.toEntity(request);
        contract.setWorker(worker);
        contract.setGuarantor(guarantor);

        if (request.getEmployerId() != null) {
            User employer = userRepository.findById(request.getEmployerId())
                    .orElseThrow(() -> new RuntimeException("Employer not found"));
            contract.setEmployer(employer);
        }

        contract = digitalContractRepository.save(contract);
        return digitalContractMapper.toDto(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DigitalContractResponseDto> getAllContracts() {
        return digitalContractRepository.findAll().stream()
                .map(digitalContractMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DigitalContractResponseDto getContractById(Long id) {
        DigitalContract contract = digitalContractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        return digitalContractMapper.toDto(contract);
    }

    @Override
    public List<DigitalContractResponseDto> getContractsByWorkerId(UUID workerId) {
        // Assuming repository has this method, otherwise implementation would differ
        return digitalContractRepository.findAll().stream()
                .filter(c -> c.getWorker().getId().equals(workerId))
                .map(digitalContractMapper::toDto)
                .collect(Collectors.toList());
    }
}
