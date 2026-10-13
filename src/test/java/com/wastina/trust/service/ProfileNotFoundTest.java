package com.wastina.trust.service;

import com.wastina.trust.mapper.EmployerProfileMapper;
import com.wastina.trust.mapper.GuarantorProfileMapper;
import com.wastina.trust.mapper.WorkerProfileMapper;
import com.wastina.trust.repository.EmployerProfileRepository;
import com.wastina.trust.repository.GuarantorProfileRepository;
import com.wastina.trust.repository.LocationRepository;
import com.wastina.trust.repository.ProfessionRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.repository.WorkerProfileRepository;
import com.wastina.trust.service.impl.EmployerProfileServiceImpl;
import com.wastina.trust.service.impl.GuarantorProfileServiceImpl;
import com.wastina.trust.service.impl.WorkerProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileNotFoundTest {
    private final UUID userId = UUID.randomUUID();

    @Test
    void missingWorkerProfileIsNotFound() {
        WorkerProfileRepository profiles = mock(WorkerProfileRepository.class);
        when(profiles.findById(userId)).thenReturn(Optional.empty());
        var service = new WorkerProfileServiceImpl(profiles, mock(UserRepository.class),
                mock(ProfessionRepository.class), mock(LocationRepository.class), mock(WorkerProfileMapper.class));

        assertNotFound(() -> service.getProfileByUserId(userId));
    }

    @Test
    void missingEmployerProfileIsNotFound() {
        EmployerProfileRepository profiles = mock(EmployerProfileRepository.class);
        when(profiles.findById(userId)).thenReturn(Optional.empty());
        var service = new EmployerProfileServiceImpl(profiles, mock(UserRepository.class),
                mock(LocationRepository.class), mock(EmployerProfileMapper.class));

        assertNotFound(() -> service.getProfileByUserId(userId));
    }

    @Test
    void missingGuarantorProfileIsNotFound() {
        GuarantorProfileRepository profiles = mock(GuarantorProfileRepository.class);
        when(profiles.findById(userId)).thenReturn(Optional.empty());
        var service = new GuarantorProfileServiceImpl(profiles, mock(UserRepository.class), mock(GuarantorProfileMapper.class));

        assertNotFound(() -> service.getProfileByUserId(userId));
    }

    private void assertNotFound(Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}