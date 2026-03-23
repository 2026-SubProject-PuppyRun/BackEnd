package org.zerock.puppyrun.pet.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.pet.DTO.UpdatePetDTO;
import org.zerock.puppyrun.pet.controller.request.RegisterPetRequest;
import org.zerock.puppyrun.pet.controller.request.UpdatePetRequest;
import org.zerock.puppyrun.pet.controller.response.PetDetailResponse;
import org.zerock.puppyrun.pet.controller.response.PetUpdateResponse;
import org.zerock.puppyrun.pet.entity.Breed;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.statistics.service.PetStatistics;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PetCommandService {
    private final PetRepository petRepository;
    private final MemberRepository memberRepository;
    private final PetStatistics petStatistics;

    /**
     * 새로운 펫을 등록합니다.
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @param request       펫 등록 요청 DTO (이름, 생년월일, 견종, 색상, 몸무게 등 포함)
     * @return 등록된 펫의 상세 정보 응답 DTO
     */
    public PetDetailResponse registerPet(UserPrincipal userPrincipal, RegisterPetRequest request) {
        Member member = memberRepository.findByIdOrThrow(userPrincipal.id());
        Breed breed = Breed.fromCode(request.breedCode());

        Pet newPet = Pet.builder()
                .member(member)
                .name(request.name())
                .birthYear(request.birthYear())
                .breed(breed)
                .color(request.color())
                .weight(request.weight())
                .isNeutered(request.isNeutered())
                .gender(request.gender())
                .build();
        petRepository.save(newPet);
        petStatistics.savePetWeightLog(newPet, request.weight());

        return PetDetailResponse.of(newPet, 0);
    }

    /**
     * 기존 펫의 정보를 수정합니다. 수정 시 펫 통계 서비스에 몸무게 변경 로그를 저장합니다.
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @param petId         수정할 펫의 ID
     * @param request       펫 수정 요청 DTO (이름, 색상, 몸무게 등 포함)
     * @return 수정된 펫의 정보 응답 DTO
     */
    public PetUpdateResponse updatePet(UserPrincipal userPrincipal, UUID petId, UpdatePetRequest request) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());

        UpdatePetDTO dto = UpdatePetDTO.builder()
                .color(request.color())
                .name(request.name())
                .weight(request.weight())
                .isNeutered(request.isNeutered())
                .birthYear(request.birthYear())
                .gender(request.gender())
                .build();
        pet.updatePet(dto);
        petRepository.save(pet);

        // 현재 몸무게와 비교 후 저장
        petStatistics.savePetWeightLog(pet, request.weight());

        return PetUpdateResponse.of(pet);
    }

    /**
     * 펫을 삭제합니다. 소유권 검증 후 삭제가 진행됩니다.
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @param petId         삭제할 펫의 ID
     */
    public void deletePet(UserPrincipal userPrincipal, UUID petId) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        petRepository.deleteById(petId);
    }
}
