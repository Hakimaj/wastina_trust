package com.wastina.trust.service.impl;

import com.wastina.trust.controller.EntityNotFoundException;
import com.wastina.trust.dto.DigitalContractRequestDto;
import com.wastina.trust.dto.DigitalContractResponseDto;
import com.wastina.trust.entity.DigitalContract;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.DigitalContractRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.service.ContractIntegrityService;
import com.wastina.trust.service.DigitalContractService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigitalContractServiceImpl implements DigitalContractService {

    private final DigitalContractRepository digitalContractRepository;
    private final UserRepository userRepository;
    private final ContractIntegrityService contractIntegrityService;

    public DigitalContractServiceImpl(DigitalContractRepository digitalContractRepository,
                                     UserRepository userRepository,
                                     ContractIntegrityService contractIntegrityService) {
        this.digitalContractRepository = digitalContractRepository;
        this.userRepository = userRepository;
        this.contractIntegrityService = contractIntegrityService;
    }

    @Override
    @Transactional
    public DigitalContractResponseDto createContractForWorker(UUID workerId,
                                                              DigitalContractRequestDto request) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new EntityNotFoundException("Worker was not found: " + workerId));
        User guarantor = userRepository.findById(request.guarantorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Guarantor was not found: " + request.guarantorId()));

        if (guarantor.getId().equals(workerId)) {
            throw new IllegalArgumentException("A user cannot be their own guarantor.");
        }

        DigitalContract contract = new DigitalContract();
        contract.setWorker(worker);
        contract.setGuarantor(guarantor);
        contract.setLiabilityAmount(request.liabilityAmount());

        if (request.employerId() != null) {
            contract.setEmployer(userRepository.findById(request.employerId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Employer was not found: " + request.employerId())));
        }

        // The terms hash is derived from the contract content, never supplied by the client, so it
        // actually attests to the terms rather than merely recording a caller-chosen value.
        contract.setTermsHash(computeTermsHash(contract));

        return DigitalContractResponseDto.from(digitalContractRepository.save(contract));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DigitalContractResponseDto> listContractsInvolvingUser(UUID userId, Pageable pageable) {
        return digitalContractRepository.findInvolvingUser(userId, pageable)
                .map(DigitalContractResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public DigitalContractResponseDto getContractForUser(Long id, UUID userId) {
        DigitalContract contract = digitalContractRepository.findById(id)
                .filter(candidate -> isParty(candidate, userId))
                .orElseThrow(() -> new EntityNotFoundException("Contract was not found: " + id));
        return DigitalContractResponseDto.from(contract);
    }

    private boolean isParty(DigitalContract contract, UUID userId) {
        return contract.getWorker().getId().equals(userId)
                || contract.getGuarantor().getId().equals(userId)
                || (contract.getEmployer() != null && contract.getEmployer().getId().equals(userId));
    }

    private String computeTermsHash(DigitalContract contract) {
        Map<String, Object> terms = new LinkedHashMap<>();
        terms.put("workerId", contract.getWorker().getId().toString());
        terms.put("guarantorId", contract.getGuarantor().getId().toString());
        terms.put("employerId", contract.getEmployer() == null ? null : contract.getEmployer().getId().toString());
        terms.put("liabilityAmount", contract.getLiabilityAmount() == null
                ? null : contract.getLiabilityAmount().toPlainString());
        terms.put("currency", "ETB");
        return contractIntegrityService.sha256Hex(contractIntegrityService.canonicalize(terms));
    }
}