package com.wastina.trust.service.impl;

import com.wastina.trust.controller.EntityNotFoundException;
import com.wastina.trust.dto.GuarantorProfileRequestDto;
import com.wastina.trust.dto.GuarantorProfileResponseDto;
import com.wastina.trust.entity.GuarantorProfile;
import com.wastina.trust.entity.User;
import com.wastina.trust.mapper.GuarantorProfileMapper;
import com.wastina.trust.repository.GuarantorProfileRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.service.GuarantorProfileService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GuarantorProfileServiceImpl implements GuarantorProfileService {

    private final GuarantorProfileRepository guarantorProfileRepository;
    private final UserRepository userRepository;
    private final GuarantorProfileMapper guarantorProfileMapper;

    public GuarantorProfileServiceImpl(GuarantorProfileRepository guarantorProfileRepository,
                                       UserRepository userRepository,
                                       GuarantorProfileMapper guarantorProfileMapper) {
        this.guarantorProfileRepository = guarantorProfileRepository;
        this.userRepository = userRepository;
        this.guarantorProfileMapper = guarantorProfileMapper;
    }

    @Override
    @Transactional
    public GuarantorProfileResponseDto createOrUpdateProfile(UUID userId, GuarantorProfileRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        GuarantorProfile profile = guarantorProfileRepository.findById(userId)
                .orElseGet(() -> new GuarantorProfile(user));

        profile.setFinancialLimit(request.financialLimit());
        profile.setEmployerName(request.employerName());

        // kycStatus is intentionally NOT written here. It is a trust signal maintained by the
        // identity verification flow and must not be settable by the profile owner.

        profile = guarantorProfileRepository.save(profile);
        return guarantorProfileMapper.toDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public GuarantorProfileResponseDto getProfileByUserId(UUID userId) {
        GuarantorProfile profile = guarantorProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guarantor profile not found."));
        return guarantorProfileMapper.toDto(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!guarantorProfileRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guarantor profile not found.");
        }
        guarantorProfileRepository.deleteById(userId);
    }
}