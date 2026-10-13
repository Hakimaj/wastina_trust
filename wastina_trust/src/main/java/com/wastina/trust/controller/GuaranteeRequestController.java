package com.wastina.trust.controller;

import com.wastina.trust.dto.GuaranteeRequestCreate;
import com.wastina.trust.dto.GuaranteeRequestView;
import com.wastina.trust.entity.GuaranteeRequest;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.GuaranteeRequestRepository;
import com.wastina.trust.security.CurrentUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import com.wastina.trust.web.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guarantee requests (the SMS-link flow).
 *
 * <p>The audit found every endpoint here accepted an arbitrary {@code requesterId}, so any
 * authenticated user could read another user's requests. The requester is now always derived from
 * the JWT. Looking a request up by id returns {@code 404} rather than {@code 403} when it belongs to
 * somebody else, so existence is not disclosed.
 */
@RestController
@RequestMapping("/api/v1/me/guarantee-requests")
@Tag(name = "Guarantees", description = "Guarantee requests")
public class GuaranteeRequestController {

    private static final int MAX_PAGE_SIZE = 200;

    private final GuaranteeRequestRepository guaranteeRequestRepository;
    private final CurrentUserResolver currentUserResolver;

    public GuaranteeRequestController(GuaranteeRequestRepository guaranteeRequestRepository,
                                      CurrentUserResolver currentUserResolver) {
        this.guaranteeRequestRepository = guaranteeRequestRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Request a guarantee from a guarantor by phone number")
    public ResponseEntity<GuaranteeRequestView> createRequest(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GuaranteeRequestCreate request) {
        User requester = currentUserResolver.requireUser(jwt);

        GuaranteeRequest guaranteeRequest = new GuaranteeRequest();
        guaranteeRequest.setRequester(requester);
        guaranteeRequest.setGuarantorPhone(request.guarantorPhone().trim());
        guaranteeRequest.setStatus(com.wastina.trust.entity.EGuaranteeStatus.PENDING);

        GuaranteeRequest saved = guaranteeRequestRepository.save(guaranteeRequest);
        return ResponseEntity
                .created(URI.create("/api/v1/me/guarantee-requests/" + saved.getId()))
                .body(GuaranteeRequestView.from(saved));
    }

    @GetMapping
    @Operation(summary = "List the caller's guarantee requests (paged)")
    public PageResponse<GuaranteeRequestView> listMyRequests(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID requesterId = currentUserResolver.requireUser(jwt).getId();
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        var result = guaranteeRequestRepository.findByRequesterId(
                requesterId, PageRequest.of(Math.max(page, 0), pageSize));

        return PageResponse.of(result,
                result.getContent().stream().map(GuaranteeRequestView::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read one of the caller's guarantee requests")
    public GuaranteeRequestView getRequestById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        UUID requesterId = currentUserResolver.requireUser(jwt).getId();

        GuaranteeRequest request = guaranteeRequestRepository.findById(id)
                .filter(candidate -> candidate.getRequester() != null
                        && candidate.getRequester().getId().equals(requesterId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Guarantee request was not found: " + id));

        return GuaranteeRequestView.from(request);
    }

    }