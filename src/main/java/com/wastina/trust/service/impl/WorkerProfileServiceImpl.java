package com.wastina.trust.service.impl;

import com.wastina.trust.dto.WorkerProfileRequestDto;
import com.wastina.trust.dto.WorkerProfileResponseDto;
import com.wastina.trust.entity.Location;
import com.wastina.trust.entity.Profession;
import com.wastina.trust.entity.User;
import com.wastina.trust.entity.WorkerProfile;
import com.wastina.trust.mapper.WorkerProfileMapper;
import com.wastina.trust.repository.LocationRepository;
import com.wastina.trust.repository.ProfessionRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.repository.WorkerProfileRepository;
import com.wastina.trust.service.WorkerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkerProfileServiceImpl implements WorkerProfileService {

    private final WorkerProfileRepository workerProfileRepository;
    private final UserRepository userRepository;
    private final ProfessionRepository professionRepository;
    private final LocationRepository locationRepository;
    private final WorkerProfileMapper workerProfileMapper;

    @Override
    @Transactional
    public WorkerProfileResponseDto createOrUpdateProfile(UUID userId, WorkerProfileRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WorkerProfile profile = workerProfileRepository.findById(userId)
                .orElse(new WorkerProfile(user));

        profile.setAvailable(request.isAvailable());

        if (request.getProfessionName() != null) {
            Profession profession = professionRepository.findByName(request.getProfessionName())
                    .orElseGet(() -> professionRepository.save(new Profession(request.getProfessionName())));
            profile.setProfession(profession);
        }

        if (request.getLocationName() != null) {
            Location location = locationRepository.findByName(request.getLocationName())
                    .orElseGet(() -> locationRepository.save(new Location(request.getLocationName())));
            profile.setLocation(location);
        }

        profile = workerProfileRepository.save(profile);
        return workerProfileMapper.toDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerProfileResponseDto getProfileByUserId(UUID userId) {
        WorkerProfile profile = workerProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Worker profile not found for user: " + userId));
        return workerProfileMapper.toDto(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!workerProfileRepository.existsById(userId)) {
            throw new RuntimeException("Worker profile not found for user: " + userId);
        }
        workerProfileRepository.deleteById(userId);
    }
}
