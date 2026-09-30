package com.wastina.trust.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ContractIntegrityService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String canonicalize(Map<String, Object> contractData) {
        Map<String, Object> sorted = new TreeMap<>();
        sorted.putAll(contractData);
        try {
            return OBJECT_MAPPER.writeValueAsString(sorted);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not canonicalize contract data", e);
        }
    }

    public String sha256Hex(String canonicalJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public boolean verifyDigest(String canonicalJson, String expectedHash) {
        return expectedHash != null && expectedHash.equals(sha256Hex(canonicalJson));
    }
}
