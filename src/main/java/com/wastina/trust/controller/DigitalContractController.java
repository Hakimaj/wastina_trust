package com.wastina.trust.controller;

import com.wastina.trust.dto.DigitalContractRequestDto;
import com.wastina.trust.dto.DigitalContractResponseDto;
import com.wastina.trust.service.DigitalContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class DigitalContractController {

    private final DigitalContractService digitalContractService;

    @PostMapping
    public ResponseEntity<DigitalContractResponseDto> createContract(@RequestBody DigitalContractRequestDto request) {
        DigitalContractResponseDto response = digitalContractService.createContract(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DigitalContractResponseDto>> getAllContracts() {
        List<DigitalContractResponseDto> response = digitalContractService.getAllContracts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DigitalContractResponseDto> getContractById(@PathVariable Long id) {
        DigitalContractResponseDto response = digitalContractService.getContractById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<DigitalContractResponseDto>> getContractsByWorker(@PathVariable UUID workerId) {
        List<DigitalContractResponseDto> response = digitalContractService.getContractsByWorkerId(workerId);
        return ResponseEntity.ok(response);
    }
}
