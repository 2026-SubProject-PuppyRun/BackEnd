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
import org.zerock.puppyrun.care.controller.request.RegisterMedicationRequest;
import org.zerock.puppyrun.care.controller.request.UpdateMedicationRequest;
import org.zerock.puppyrun.care.controller.response.MedicationListResponse;
import org.zerock.puppyrun.care.controller.response.MedicationRecordResponse;
import org.zerock.puppyrun.care.service.MedicationCommandService;
import org.zerock.puppyrun.care.service.MedicationQueryService;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;

@RestController
@RequestMapping("/api/pets/{petId}/medication-logs")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationCommandService medicationCommandService;
    private final MedicationQueryService medicationQueryService;

    @PostMapping
    public ResponseEntity<MedicationRecordResponse> registerMedication(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @RequestBody @Valid RegisterMedicationRequest request
    ) {
        MedicationRecordResponse response = medicationCommandService.registerMedication(userPrincipal, petId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<MedicationListResponse> getMedicationList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId
    ) {
        MedicationListResponse response = medicationQueryService.getMedicationList(userPrincipal, petId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{medicationLogId}")
    public ResponseEntity<MedicationRecordResponse> updateMedication(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @PathVariable UUID medicationLogId,
            @RequestBody @Valid UpdateMedicationRequest request
    ) {
        MedicationRecordResponse response = medicationCommandService.updateMedication(
                userPrincipal,
                petId,
                medicationLogId,
                request
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{medicationLogId}")
    public ResponseEntity<Void> deleteMedication(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @PathVariable UUID medicationLogId
    ) {
        medicationCommandService.deleteMedication(userPrincipal, petId, medicationLogId);
        return ResponseEntity.ok().build();
    }
}
