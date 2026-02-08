package com.wastina.trust.service;

import com.wastina.trust.dto.EmployerProfileRequestDto;
import com.wastina.trust.dto.EmployerProfileResponseDto;
import java.util.UUID;

public interface EmployerProfileService {
    EmployerProfileResponseDto createOrUpdateProfile(UUID userId, EmployerProfileRequestDto request);

    EmployerProfileResponseDto getProfileByUserId(UUID userId);

    void deleteProfile(UUID userId);
}
