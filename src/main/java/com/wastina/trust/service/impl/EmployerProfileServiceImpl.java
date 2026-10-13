package com.wastina.trust.service.impl;

import com.wastina.trust.dto.EmployerProfileRequestDto;
import com.wastina.trust.dto.EmployerProfileResponseDto;
import com.wastina.trust.entity.EmployerProfile;
import com.wastina.trust.entity.Location;
import com.wastina.trust.entity.User;
import com.wastina.trust.mapper.EmployerProfileMapper;
import com.wastina.trust.repository.EmployerProfileRepository;
import com.wastina.trust.repository.LocationRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.service.EmployerProfileService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployerProfileServiceImpl implements EmployerProfileService {

    private final EmployerProfileRepository employerProfileRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final EmployerProfileMapper employerProfileMapper;

    public EmployerProfileServiceImpl(EmployerProfileRepository employerProfileRepository,
                                     UserRepository userRepository,
                                     LocationRepository locationRepository,
                                     EmployerProfileMapper employerProfileMapper) {
        this.employerProfileRepository = employerProfileRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.employerProfileMapper = employerProfileMapper;
    }

    @Override
    @Transactional
    public EmployerProfileResponseDto createOrUpdateProfile(UUID userId, EmployerProfileRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        EmployerProfile profile = employerProfileRepository.findById(userId)
                .orElseGet(() -> new EmployerProfile(user));

        profile.setCompanyName(request.companyName());
        profile.setSubscriptionTier(normalizeTier(request.subscriptionTier()));

        if (request.locationName() != null) {
            Location location = locationRepository.findByName(request.locationName())
                    .orElseGet(() -> locationRepository.save(new Location(request.locationName())));
            profile.setLocation(location);
        }

        profile = employerProfileRepository.save(profile);
        return employerProfileMapper.toDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployerProfileResponseDto getProfileByUserId(UUID userId) {
        EmployerProfile profile = employerProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employer profile not found."));
        return employerProfileMapper.toDto(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!employerProfileRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employer profile not found.");
        }
        employerProfileRepository.deleteById(userId);
    }

    private String normalizeTier(String tier) {
        return tier == null ? null : tier.trim().toUpperCase(java.util.Locale.ROOT);
    }
}