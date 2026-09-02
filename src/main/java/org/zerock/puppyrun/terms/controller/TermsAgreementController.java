package org.zerock.puppyrun.terms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.terms.controller.request.TermsAgreementRequest;
import org.zerock.puppyrun.terms.controller.response.TermsAgreementStatusResponse;
import org.zerock.puppyrun.terms.service.TermsAgreementCommandService;
import org.zerock.puppyrun.terms.service.TermsAgreementQueryService;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsAgreementController {

    private final TermsAgreementCommandService termsAgreementCommandService;
    private final TermsAgreementQueryService termsAgreementQueryService;

    @GetMapping("/status")
    public ResponseEntity<TermsAgreementStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(termsAgreementQueryService.getStatus(userPrincipal.id()));
    }

    @PostMapping("/agreements")
    public ResponseEntity<Void> agree(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody TermsAgreementRequest request
    ) {
        termsAgreementCommandService.agree(userPrincipal, request);
        return ResponseEntity.ok().build();
    }
}
