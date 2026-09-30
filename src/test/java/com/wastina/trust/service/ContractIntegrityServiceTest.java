package com.wastina.trust.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContractIntegrityServiceTest {
    private final ContractIntegrityService service = new ContractIntegrityService();

    @Test
    void hashChangesWhenContractChanges() {
        Map<String, Object> original = Map.of(
                "amount", "50000.00",
                "currency", "ETB",
                "guarantorId", "guarantor-1",
                "purpose", "RENT"
        );
        Map<String, Object> tampered = Map.of(
                "amount", "500000.00",
                "currency", "ETB",
                "guarantorId", "guarantor-1",
                "purpose", "RENT"
        );

        String originalHash = service.sha256Hex(service.canonicalize(original));
        String tamperedHash = service.sha256Hex(service.canonicalize(tampered));

        assertNotEquals(originalHash, tamperedHash);
        assertFalse(service.verifyDigest(service.canonicalize(tampered), originalHash));
    }
}
