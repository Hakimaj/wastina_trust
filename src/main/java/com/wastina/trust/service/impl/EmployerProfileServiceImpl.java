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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployerProfileServiceImpl implements EmployerProfileService {

    private final EmployerProfileRepository employerProfileRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final EmployerProfileMapper employerProfileMapper;

    @Override
    @Transactional
    public EmployerProfileResponseDto createOrUpdateProfile(UUID userId, EmployerProfileRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        EmployerProfile profile = employerProfileRepository.findById(userId)
                .orElse(new EmployerProfile(user));

        profile.setCompanyName(request.getCompanyName());
        profile.setSubscriptionTier(request.getSubscriptionTier());

        if (request.getLocationName() != null) {
            Location location = locationRepository.findByName(request.getLocationName())
                    .orElseGet(() -> locationRepository.save(new Location(request.getLocationName())));
            profile.setLocation(location);
        }

        profile = employerProfileRepository.save(profile);
        return employerProfileMapper.toDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployerProfileResponseDto getProfileByUserId(UUID userId) {
        EmployerProfile profile = employerProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Employer profile not found for user: " + userId));
        return employerProfileMapper.toDto(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!employerProfileRepository.existsById(userId)) {
            throw new RuntimeException("Employer profile not found for user: " + userId);
        }
        employerProfileRepository.deleteById(userId);
    }
}
