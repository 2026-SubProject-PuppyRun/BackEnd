package org.zerock.puppyrun.care.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.care.controller.request.RegisterVaccinationRequest;
import org.zerock.puppyrun.care.controller.request.UpdateVaccinationRequest;
import org.zerock.puppyrun.care.controller.response.VaccinationListResponse;
import org.zerock.puppyrun.care.controller.response.VaccinationRecordResponse;
import org.zerock.puppyrun.care.service.VaccinationCommandService;
import org.zerock.puppyrun.care.service.VaccinationQueryService;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;

@RestController
@RequestMapping("/api/pets/{petId}/vaccinations")
@RequiredArgsConstructor
public class VaccinationController {

    private final VaccinationCommandService vaccinationCommandService;
    private final VaccinationQueryService vaccinationQueryService;

    @PostMapping
    public ResponseEntity<VaccinationRecordResponse> registerVaccination(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @RequestBody @Valid RegisterVaccinationRequest request
    ) {
        VaccinationRecordResponse response = vaccinationCommandService.registerVaccination(userPrincipal, petId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<VaccinationListResponse> getVaccinationList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId
    ) {
        VaccinationListResponse response = vaccinationQueryService.getVaccinationList(userPrincipal, petId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{vaccinationId}")
    public ResponseEntity<VaccinationRecordResponse> updateVaccination(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @PathVariable UUID vaccinationId,
            @RequestBody @Valid UpdateVaccinationRequest request
    ) {
        VaccinationRecordResponse response = vaccinationCommandService.updateVaccination(
                userPrincipal,
                petId,
                vaccinationId,
                request
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{vaccinationId}")
    public ResponseEntity<Void> deleteVaccination(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @PathVariable UUID vaccinationId
    ) {
        vaccinationCommandService.deleteVaccination(userPrincipal, petId, vaccinationId);
        return ResponseEntity.ok().build();
    }
}
