package org.zerock.puppyrun.pet.controller;

import jakarta.validation.Valid;
import java.util.List;
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
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.pet.controller.request.RegisterPetRequest;
import org.zerock.puppyrun.pet.controller.request.UpdatePetRequest;
import org.zerock.puppyrun.pet.controller.response.PetDetailResponse;
import org.zerock.puppyrun.pet.controller.response.PetListResponse;
import org.zerock.puppyrun.pet.controller.response.PetUpdateResponse;
import org.zerock.puppyrun.pet.controller.response.PetWeightLogResponse;
import org.zerock.puppyrun.pet.service.PetCommandService;
import org.zerock.puppyrun.pet.service.PetQueryService;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetCommandService petCommandService;
    private final PetQueryService petQueryService;

    /**
     * 펫 등록
     */
    @PostMapping
    public ResponseEntity<PetDetailResponse> registerPet(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid RegisterPetRequest request
    ) {
        // Service에서 UserPrincipal을 직접 받도록 정의되어 있음
        PetDetailResponse response = petCommandService.registerPet(userPrincipal, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 펫 목록 조회
     */
    @GetMapping
    public ResponseEntity<PetListResponse> getPetList(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        PetListResponse response = petQueryService.getPetList(userPrincipal);
        return ResponseEntity.ok(response);
    }

    /**
     * 펫 상세 조회
     */
    @GetMapping("/{petId}")
    public ResponseEntity<PetDetailResponse> getPet(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId
    ) {
        PetDetailResponse response = petQueryService.getPet(userPrincipal, petId);
        return ResponseEntity.ok(response);
    }

    /**
     * 펫 정보 수정
     */
    @PutMapping("/{petId}")
    public ResponseEntity<PetUpdateResponse> updatePet(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @RequestBody @Valid UpdatePetRequest request
    ) {
        PetUpdateResponse response = petCommandService.updatePet(userPrincipal, petId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 펫 삭제
     */
    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> deletePet(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId
    ) {
        petCommandService.deletePet(userPrincipal, petId);
        return ResponseEntity.ok().build();
    }

    /**
     * 펫 몸무게 기록 조회
     */
    @GetMapping("/{petId}/weight-logs")
    public ResponseEntity<PetWeightLogResponse> getPetWeightLogs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId
    ) {
        PetWeightLogResponse response = petQueryService.getPetWeightLog(userPrincipal, petId);
        return ResponseEntity.ok(response);
    }

}
