package org.zerock.puppyrun.pet.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.pet.controller.response.PetDetailResponse;
import org.zerock.puppyrun.pet.controller.response.PetListResponse;
import org.zerock.puppyrun.pet.controller.response.PetProgressResponse;
import org.zerock.puppyrun.pet.controller.response.PetWeightLogResponse;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetWeightLog;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.statistics.service.PetStatistics;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetQueryService {
    private final PetRepository petRepository;
    private final PetStatistics petStatistics;

    /**
     * 펫의 상세 정보를 조회합니다. 펫 통계 서비스에서 누적 산책 거리를 조회하여 함께 반환합니다.
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @param petId         조회할 펫의 ID
     * @return 펫 상세 정보 응답 DTO (기본 정보 + 통계 정보 포함)
     */
    public PetDetailResponse getPet(UserPrincipal userPrincipal, UUID petId) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        return PetDetailResponse.of(pet, petStatistics.getTotalWalkedDistance(pet));
    }

    /**
     * 사용자가 소유한 모든 펫의 목록을 조회합니다.
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @return 펫 목록 응답 DTO
     */
    public PetListResponse getPetList(UserPrincipal userPrincipal) {
        List<Pet> petList = petRepository.findAllByMemberId(userPrincipal.id());
        return PetListResponse.of(petList);
    }

    /**
     * 사용자가 소유한 펫의 몸무게 로그를 조회합니다.
     */
    public PetWeightLogResponse getPetWeightLog(UserPrincipal userPrincipal, UUID petId) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        List<PetWeightLog> petWeightLog = petStatistics.getPetWeightLog(pet.getId());
        return PetWeightLogResponse.of(pet, petWeightLog);
    }

    /**
     * 쿼리 파라미터로 전달된 펫 ID 개수에 따라 전체, 단건 또는 복수 펫의 진행도를 조회합니다.
     */
    public PetProgressResponse getPetProgress(UserPrincipal userPrincipal, List<UUID> petIds) {
        // 사용자 소유 펫 전체 조회
        if (petIds == null || petIds.isEmpty()) {
            List<Pet> pets = petRepository.findAllByMemberId(userPrincipal.id());
            return progressResponse(pets);
        }

        // 중복 제거
        List<UUID> distinctPetIds = petIds.stream()
                .distinct()
                .toList();

        //단건 조회
        if (distinctPetIds.size() == 1) {
            Pet pet = petRepository.findByIdAndVerifyOwnership(
                    distinctPetIds.getFirst(),
                    userPrincipal.id()
            );
            return progressResponse(List.of(pet));
        }

        // 복수 조회
        List<Pet> pets = petRepository.findAllByMemberIdAndIdIn(userPrincipal.id(), distinctPetIds);
        Map<UUID, Pet> petById = pets.stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));

        if (petById.size() != distinctPetIds.size()) {
            throw new ResourceNotFoundException("조회할 수 없는 펫이 포함되어 있습니다.");
        }

        List<Pet> orderedPets = distinctPetIds.stream()
                .map(petById::get)
                .toList();

        return progressResponse(orderedPets);
    }

    private PetProgressResponse progressResponse(List<Pet> pets) {
        if (pets.isEmpty()) {
            return PetProgressResponse.from(List.of(), Map.of());
        }

        Map<UUID, Integer> walkedDistances = petStatistics.getTotalWalkedDistances(
                pets.stream().map(Pet::getId).toList()
        );
        return PetProgressResponse.from(pets, walkedDistances);
    }

}
