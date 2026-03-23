package org.zerock.puppyrun.pet.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.pet.controller.response.PetDetailResponse;
import org.zerock.puppyrun.pet.controller.response.PetListResponse;
import org.zerock.puppyrun.pet.controller.response.PetWeightLogResponse;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetWeightLog;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.statistics.service.PetStatistics;
import org.zerock.puppyrun.statistics.service.TrackingStatistics;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetQueryService {
    private final PetRepository petRepository;
    private final PetStatistics petStatistics;
    private final TrackingStatistics TrackingStatistics;

    /**
     * 펫의 상세 정보를 조회합니다. 펫 통계 서비스에서 누적 산책 거리를 조회하여 함께 반환합니다.
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @param petId         조회할 펫의 ID
     * @return 펫 상세 정보 응답 DTO (기본 정보 + 통계 정보 포함)
     */
    public PetDetailResponse getPet(UserPrincipal userPrincipal, UUID petId) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        int walkedDistance = TrackingStatistics.getTotalWalkedDistance(pet); // 누적 산책거리 조회
        return PetDetailResponse.of(pet, walkedDistance);
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
}
