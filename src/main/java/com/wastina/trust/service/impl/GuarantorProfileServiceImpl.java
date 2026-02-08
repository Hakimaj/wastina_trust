package com.wastina.trust.service.impl;

import com.wastina.trust.dto.GuarantorProfileRequestDto;
import com.wastina.trust.dto.GuarantorProfileResponseDto;
import com.wastina.trust.entity.GuarantorProfile;
import com.wastina.trust.entity.User;
import com.wastina.trust.mapper.GuarantorProfileMapper;
import com.wastina.trust.repository.GuarantorProfileRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.service.GuarantorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuarantorProfileServiceImpl implements GuarantorProfileService {

    private final GuarantorProfileRepository guarantorProfileRepository;
    private final UserRepository userRepository;
    private final GuarantorProfileMapper guarantorProfileMapper;

    @Override
    @Transactional
    public GuarantorProfileResponseDto createOrUpdateProfile(UUID userId, GuarantorProfileRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GuarantorProfile profile = guarantorProfileRepository.findById(userId)
                .orElse(new GuarantorProfile(user));

        profile.setFinancialLimit(request.getFinancialLimit());
        profile.setEmployerName(request.getEmployerName());

        if (request.getKycStatus() != null) {
            profile.setKycStatus(request.getKycStatus());
        }

        profile = guarantorProfileRepository.save(profile);
        return guarantorProfileMapper.toDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public GuarantorProfileResponseDto getProfileByUserId(UUID userId) {
        GuarantorProfile profile = guarantorProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Guarantor profile not found for user: " + userId));
        return guarantorProfileMapper.toDto(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!guarantorProfileRepository.existsById(userId)) {
            throw new RuntimeException("Guarantor profile not found for user: " + userId);
        }
        guarantorProfileRepository.deleteById(userId);
    }
}
