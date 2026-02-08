package com.wastina.trust.service;

import com.wastina.trust.dto.WorkerProfileRequestDto;
import com.wastina.trust.dto.WorkerProfileResponseDto;
import java.util.UUID;

public interface WorkerProfileService {
    WorkerProfileResponseDto createOrUpdateProfile(UUID userId, WorkerProfileRequestDto request);

    WorkerProfileResponseDto getProfileByUserId(UUID userId);

    void deleteProfile(UUID userId);
}
