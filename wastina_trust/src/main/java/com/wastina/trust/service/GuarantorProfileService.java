package com.wastina.trust.service;

import com.wastina.trust.dto.GuarantorProfileRequestDto;
import com.wastina.trust.dto.GuarantorProfileResponseDto;
import java.util.UUID;

public interface GuarantorProfileService {
    GuarantorProfileResponseDto createOrUpdateProfile(UUID userId, GuarantorProfileRequestDto request);

    GuarantorProfileResponseDto getProfileByUserId(UUID userId);

    void deleteProfile(UUID userId);
}
