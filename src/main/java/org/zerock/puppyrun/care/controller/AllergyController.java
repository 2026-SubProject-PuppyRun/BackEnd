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
import org.zerock.puppyrun.care.controller.request.RegisterAllergyRequest;
import org.zerock.puppyrun.care.controller.request.UpdateAllergyRequest;
import org.zerock.puppyrun.care.controller.response.AllergyListResponse;
import org.zerock.puppyrun.care.controller.response.AllergyRecordResponse;
import org.zerock.puppyrun.care.service.AllergyCommandService;
import org.zerock.puppyrun.care.service.AllergyQueryService;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;

@RestController
@RequestMapping("/api/pets/{petId}/allergies")
@RequiredArgsConstructor
public class AllergyController {

    private final AllergyCommandService allergyCommandService;
    private final AllergyQueryService allergyQueryService;

    @PostMapping
    public ResponseEntity<AllergyRecordResponse> registerAllergy(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @RequestBody @Valid RegisterAllergyRequest request
    ) {
        AllergyRecordResponse response = allergyCommandService.registerAllergy(userPrincipal, petId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<AllergyListResponse> getAllergyList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId
    ) {
        AllergyListResponse response = allergyQueryService.getAllergyList(userPrincipal, petId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{allergyId}")
    public ResponseEntity<AllergyRecordResponse> updateAllergy(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @PathVariable UUID allergyId,
            @RequestBody @Valid UpdateAllergyRequest request
    ) {
        AllergyRecordResponse response = allergyCommandService.updateAllergy(
                userPrincipal,
                petId,
                allergyId,
                request
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{allergyId}")
    public ResponseEntity<Void> deleteAllergy(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @PathVariable UUID allergyId
    ) {
        allergyCommandService.deleteAllergy(userPrincipal, petId, allergyId);
        return ResponseEntity.ok().build();
    }
}
