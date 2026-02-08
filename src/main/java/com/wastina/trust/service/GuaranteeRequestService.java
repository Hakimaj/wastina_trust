package com.wastina.trust.service;

import com.wastina.trust.dto.GuaranteeRequestDto;
import com.wastina.trust.dto.GuaranteeResponseDto;
import java.util.List;
import java.util.UUID;

public interface GuaranteeRequestService {
    GuaranteeResponseDto createRequest(GuaranteeRequestDto request);

    List<GuaranteeResponseDto> getRequestsByRequesterId(UUID requesterId);

    GuaranteeResponseDto getRequestById(Long id);
}
