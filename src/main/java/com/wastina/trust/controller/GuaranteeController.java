package com.wastina.trust.controller;

import com.wastina.trust.dto.GuaranteeCreateRequest;
import com.wastina.trust.dto.GuaranteeView;
import com.wastina.trust.entity.User;
import com.wastina.trust.security.CurrentUserResolver;
import com.wastina.trust.service.GuaranteeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Digital guarantees.
 *
 * <p>Changes from the audited original:
 * <ul>
 *   <li>The guarantee is actually persisted and returned with {@code 201 Created}. Previously the
 *       controller built an entity, discarded it and returned {@code 200 OK} with a fabricated
 *       identifier.</li>
 *   <li>The principal is the authenticated caller. The previous endpoint read {@code principalId}
 *       from the body, so any user could create a guarantee in another party's name.</li>
 *   <li>Reads are restricted to the parties of the guarantee, and listing is scoped to the caller.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/guarantees")
@Tag(name = "Guarantees", description = "Digital guarantees")
public class GuaranteeController {

    private final GuaranteeService guaranteeService;
    private final CurrentUserResolver currentUserResolver;

    public GuaranteeController(GuaranteeService guaranteeService, CurrentUserResolver currentUserResolver) {
        this.guaranteeService = guaranteeService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    @Operation(summary = "Create a guarantee; the caller is always the principal")
    public ResponseEntity<GuaranteeView> createGuarantee(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GuaranteeCreateRequest request) {
        User principal = currentUserResolver.requireUser(jwt);
        var guarantee = guaranteeService.createGuarantee(principal, request);
        return ResponseEntity
                .created(URI.create("/api/v1/guarantees/" + guarantee.getPublicReference()))
                .body(GuaranteeView.from(guarantee));
    }

    @GetMapping
    @Operation(summary = "List the caller's guarantees")
    public List<GuaranteeView> listMyGuarantees(@AuthenticationPrincipal Jwt jwt) {
        User user = currentUserResolver.requireUser(jwt);
        return guaranteeService.listForUser(user.getId()).stream()
                .map(GuaranteeView::from)
                .toList();
    }

    @GetMapping("/{publicReference}")
    @Operation(summary = "Read a guarantee the caller is a party to")
    public GuaranteeView getGuarantee(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String publicReference) {
        User user = currentUserResolver.requireUser(jwt);
        return GuaranteeView.from(guaranteeService.getForUser(publicReference, user));
    }

    @PostMapping("/{publicReference}/accept")
    @Operation(summary = "Accept a pending guarantee (guarantor only)")
    public GuaranteeView accept(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String publicReference) {
        User user = currentUserResolver.requireUser(jwt);
        return GuaranteeView.from(
                guaranteeService.acceptGuarantee(publicReference, user));
    }

    @PostMapping("/{publicReference}/reject")
    @Operation(summary = "Reject a pending guarantee (guarantor only)")
    public GuaranteeView reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String publicReference,
            @RequestBody(required = false) RejectRequest request) {
        User user = currentUserResolver.requireUser(jwt);
        return GuaranteeView.from(guaranteeService.rejectGuarantee(publicReference, user,
                request == null ? null : request.reason()));
    }

    public record RejectRequest(String reason) {
    }
}