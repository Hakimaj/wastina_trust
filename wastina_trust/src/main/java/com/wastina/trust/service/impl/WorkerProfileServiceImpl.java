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
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkerProfileServiceImpl implements WorkerProfileService {

    private final WorkerProfileRepository workerProfileRepository;
    private final UserRepository userRepository;
    private final ProfessionRepository professionRepository;
    private final LocationRepository locationRepository;
    private final WorkerProfileMapper workerProfileMapper;

    public WorkerProfileServiceImpl(WorkerProfileRepository workerProfileRepository,
                                    UserRepository userRepository,
                                    ProfessionRepository professionRepository,
                                    LocationRepository locationRepository,
                                    WorkerProfileMapper workerProfileMapper) {
        this.workerProfileRepository = workerProfileRepository;
        this.userRepository = userRepository;
        this.professionRepository = professionRepository;
        this.locationRepository = locationRepository;
        this.workerProfileMapper = workerProfileMapper;
    }

    @Override
    @Transactional
    public WorkerProfileResponseDto createOrUpdateProfile(UUID userId, WorkerProfileRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        WorkerProfile profile = workerProfileRepository.findById(userId)
                .orElseGet(() -> new WorkerProfile(user));

        profile.setAvailable(request.isAvailable());

        if (request.professionName() != null) {
            Profession profession = professionRepository.findByName(request.professionName())
                    .orElseGet(() -> professionRepository.save(new Profession(request.professionName())));
            profile.setProfession(profession);
        }

        if (request.locationName() != null) {
            Location location = locationRepository.findByName(request.locationName())
                    .orElseGet(() -> locationRepository.save(new Location(request.locationName())));
            profile.setLocation(location);
        }

        profile = workerProfileRepository.save(profile);
        return workerProfileMapper.toDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerProfileResponseDto getProfileByUserId(UUID userId) {
        WorkerProfile profile = workerProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker profile not found."));
        return workerProfileMapper.toDto(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!workerProfileRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker profile not found.");
        }
        workerProfileRepository.deleteById(userId);
    }
}