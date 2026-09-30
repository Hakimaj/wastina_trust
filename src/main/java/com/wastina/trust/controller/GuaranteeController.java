package com.wastina.trust.controller;

import com.wastina.trust.entity.Guarantee;
import com.wastina.trust.entity.GuaranteeStatus;
import com.wastina.trust.entity.GuaranteeType;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.service.ContractIntegrityService;
import com.wastina.trust.service.GuaranteeCapacityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GuaranteeController {
    private final UserRepository userRepository;
    private final GuaranteeCapacityService guaranteeCapacityService;
    private final ContractIntegrityService contractIntegrityService;

    @PostMapping("/guarantees")
    public ResponseEntity<Map<String, Object>> createGuarantee(@RequestBody Map<String, Object> request) {
        UUID principalId = UUID.fromString((String) request.get("principalId"));
        UUID guarantorId = UUID.fromString((String) request.get("guarantorId"));
        UUID beneficiaryId = request.get("beneficiaryId") == null ? null : UUID.fromString((String) request.get("beneficiaryId"));

        User principal = userRepository.findById(principalId).orElseThrow(() -> new IllegalArgumentException("Principal not found"));
        User guarantor = userRepository.findById(guarantorId).orElseThrow(() -> new IllegalArgumentException("Guarantor not found"));
        User beneficiary = beneficiaryId == null ? null : userRepository.findById(beneficiaryId).orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));

        BigDecimal amount = new BigDecimal(String.valueOf(request.getOrDefault("amount", "0.00")));
        String currency = String.valueOf(request.getOrDefault("currency", "ETB"));
        Guarantee guarantee = new Guarantee();
        guarantee.setPublicReference("WST-" + System.currentTimeMillis());
        guarantee.setType(GuaranteeType.valueOf(String.valueOf(request.getOrDefault("type", GuaranteeType.OTHER.name()))));
        guarantee.setStatus(GuaranteeStatus.PENDING_ACCEPTANCE);
        guarantee.setPrincipal(principal);
        guarantee.setGuarantor(guarantor);
        guarantee.setBeneficiary(beneficiary);
        guarantee.setAmount(amount);
        guarantee.setCurrency(currency);
        guarantee.setGuaranteedAmount(amount);
        guarantee.setOutstandingLiability(amount);
        guarantee.setPurpose(String.valueOf(request.getOrDefault("purpose", "Guarantee")));
        guarantee.setDescription(String.valueOf(request.getOrDefault("description", "")));
        guarantee.setExpiresAt(Instant.now().plusSeconds(86400L * 30));

        var capacity = guaranteeCapacityService.evaluate(guarantor, java.util.List.of(), amount);
        if (!capacity.allowed()) {
            throw new IllegalStateException("Guarantor capacity exceeded");
        }

        Map<String, Object> contract = new HashMap<>();
        contract.put("principalId", principal.getId().toString());
        contract.put("guarantorId", guarantor.getId().toString());
        contract.put("beneficiaryId", beneficiary == null ? null : beneficiary.getId().toString());
        contract.put("amount", amount.toPlainString());
        contract.put("currency", currency);
        contract.put("purpose", guarantee.getPurpose());
        contract.put("status", guarantee.getStatus().name());
        contract.put("termsVersion", guarantee.getTermsVersion());
        contract.put("contractVersion", guarantee.getContractVersion());
        guarantee.setContractHash(contractIntegrityService.sha256Hex(contractIntegrityService.canonicalize(contract)));

        Map<String, Object> response = new HashMap<>();
        response.put("guaranteeId", guarantee.getPublicReference());
        response.put("status", guarantee.getStatus().name());
        response.put("contractHash", guarantee.getContractHash());
        response.put("capacityAllowed", capacity.allowed());
        return ResponseEntity.ok(response);
    }
}
