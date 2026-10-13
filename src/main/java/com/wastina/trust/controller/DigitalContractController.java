package com.wastina.trust.controller;

import com.wastina.trust.dto.DigitalContractRequestDto;
import com.wastina.trust.dto.DigitalContractResponseDto;
import com.wastina.trust.entity.User;
import com.wastina.trust.security.CurrentUserResolver;
import com.wastina.trust.service.DigitalContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wastina.trust.web.PageResponse;

/**
 * Digital contracts.
 *
 * <p>The audit found {@code GET /api/contracts} returned every contract in the system to any
 * authenticated caller, and that {@code /worker/{id}} allowed reading any worker's contracts. Both
 * are gone. Reads are now scoped to contracts the caller participates in.
 *
 * <p>{@code termsHash} is also no longer accepted from the client: it is computed server-side from
 * the contract terms, otherwise a caller could assert any hash they liked and defeat the point of
 * contract integrity.
 */
@RestController
@RequestMapping("/api/v1/contracts")
@Tag(name = "Contracts", description = "Digital guarantee contracts")
public class DigitalContractController {

    private static final int MAX_PAGE_SIZE = 200;

    private final DigitalContractService digitalContractService;
    private final CurrentUserResolver currentUserResolver;

    public DigitalContractController(DigitalContractService digitalContractService,
                                     CurrentUserResolver currentUserResolver) {
        this.digitalContractService = digitalContractService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    @Operation(summary = "Create a contract for the authenticated caller")
    public ResponseEntity<DigitalContractResponseDto> createContract(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DigitalContractRequestDto request) {
        UUID workerId = currentUserResolver.requireUser(jwt).getId();
        DigitalContractResponseDto response =
                digitalContractService.createContractForWorker(workerId, request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    @Operation(summary = "List contracts the caller participates in (paged)")
    public PageResponse<DigitalContractResponseDto> listMyContracts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = currentUserResolver.requireUser(jwt).getId();
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        var result = digitalContractService.listContractsInvolvingUser(
                userId, PageRequest.of(Math.max(page, 0), pageSize));

        // The service already maps entities to views, so the page content is passed through as-is.
        return PageResponse.of(result, result.getContent());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read a contract the caller participates in")
    public DigitalContractResponseDto getContractById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        UUID userId = currentUserResolver.requireUser(jwt).getId();
        return digitalContractService.getContractForUser(id, userId);
    }

    }