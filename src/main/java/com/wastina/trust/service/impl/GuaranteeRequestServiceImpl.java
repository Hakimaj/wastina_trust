package com.wastina.trust.service.impl;

import com.wastina.trust.dto.GuaranteeRequestDto;
import com.wastina.trust.dto.GuaranteeResponseDto;
import com.wastina.trust.entity.GuaranteeRequest;
import com.wastina.trust.entity.User;
import com.wastina.trust.mapper.GuaranteeRequestMapper;
import com.wastina.trust.repository.GuaranteeRequestRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.service.GuaranteeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuaranteeRequestServiceImpl implements GuaranteeRequestService {

    private final GuaranteeRequestRepository guaranteeRequestRepository;
    private final UserRepository userRepository;
    private final GuaranteeRequestMapper guaranteeRequestMapper;

    @Override
    @Transactional
    public GuaranteeResponseDto createRequest(GuaranteeRequestDto request) {
        User requester = userRepository.findById(request.getRequesterId())
                .orElseThrow(() -> new RuntimeException("Requester not found"));

        GuaranteeRequest guaranteeRequest = guaranteeRequestMapper.toEntity(request);
        guaranteeRequest.setRequester(requester);

        guaranteeRequest = guaranteeRequestRepository.save(guaranteeRequest);
        return guaranteeRequestMapper.toDto(guaranteeRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuaranteeResponseDto> getRequestsByRequesterId(UUID requesterId) {
        // Assuming repository method exists or using generic find
        return guaranteeRequestRepository.findAll().stream()
                .filter(r -> r.getRequester().getId().equals(requesterId))
                .map(guaranteeRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GuaranteeResponseDto getRequestById(Long id) {
        GuaranteeRequest request = guaranteeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guarantee request not found with id: " + id));
        return guaranteeRequestMapper.toDto(request);
    }
}
