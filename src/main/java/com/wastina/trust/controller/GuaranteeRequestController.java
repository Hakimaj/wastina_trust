package com.wastina.trust.controller;

import com.wastina.trust.dto.GuaranteeRequestDto;
import com.wastina.trust.dto.GuaranteeResponseDto;
import com.wastina.trust.service.GuaranteeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/guarantee-requests")
@RequiredArgsConstructor
public class GuaranteeRequestController {

    private final GuaranteeRequestService guaranteeRequestService;

    @PostMapping
    public ResponseEntity<GuaranteeResponseDto> createRequest(@RequestBody GuaranteeRequestDto request) {
        GuaranteeResponseDto response = guaranteeRequestService.createRequest(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<List<GuaranteeResponseDto>> getRequestsByRequester(@PathVariable UUID requesterId) {
        List<GuaranteeResponseDto> response = guaranteeRequestService.getRequestsByRequesterId(requesterId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuaranteeResponseDto> getRequestById(@PathVariable Long id) {
        GuaranteeResponseDto response = guaranteeRequestService.getRequestById(id);
        return ResponseEntity.ok(response);
    }
}
