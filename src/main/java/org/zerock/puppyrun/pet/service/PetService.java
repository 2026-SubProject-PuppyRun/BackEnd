package org.zerock.puppyrun.pet.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.pet.DTO.UpdatePetDTO;
import org.zerock.puppyrun.pet.controller.request.RegisterPetRequest;
import org.zerock.puppyrun.pet.controller.request.UpdatePetRequest;
import org.zerock.puppyrun.pet.controller.response.PetDetailResponse;
import org.zerock.puppyrun.pet.controller.response.PetListResponse;
import org.zerock.puppyrun.pet.controller.response.PetUpdateResponse;
import org.zerock.puppyrun.pet.entity.Breed;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {
    private final PetRepository petRepository;
    private final MemberRepository memberRepository;
    private final PetStatistics petStatistics;

    /**
     * 펫 ID로 펫을 조회하고, 요청한 사용자가 소유자인지 검증합니다.
     *
     * @param memberId 요청한 사용자의 ID
     * @param petId    조회할 펫의 ID
     * @return 조회된 Pet 엔티티
     * @throws ResourceNotFoundException 펫이 존재하지 않을 경우
     * @throws UserForbiddenException    사용자가 해당 펫의 소유자가 아닐 경우
     */
    private Pet findPetWithOwnershipCheck(UUID memberId, UUID petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("펫을 찾을 수 없습니다."));

        if (pet.isNotOwner(memberId)) {
            throw new UserForbiddenException("해당 펫에 대한 권한이 없습니다.");
        }

        return pet;
    }

    /**
     * 새로운 펫을 등록합니다.
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @param request       펫 등록 요청 DTO (이름, 생년월일, 견종, 색상, 몸무게 등 포함)
     * @return 등록된 펫의 상세 정보 응답 DTO
     */
    @Transactional
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
    @Transactional
    public PetUpdateResponse updatePet(UserPrincipal userPrincipal, UUID petId, UpdatePetRequest request,
                                       MultipartFile image) {
        Pet pet = findPetWithOwnershipCheck(userPrincipal.id(), petId);
        UpdatePetDTO dto = UpdatePetDTO.builder()
                .color(request.color())
                .name(request.name())
                .weight(request.weight())
                .isNeutered(request.isNeutered())
                .gender(request.gender())
                .build();
        pet.updatePet(dto);
        petRepository.save(pet);

        petStatistics.savePetWeightLog(petId, request.weight());

        return PetUpdateResponse.of(pet);
    }

    /**
     * 펫을 삭제합니다. 소유권 검증 후 삭제가 진행됩니다.
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @param petId         삭제할 펫의 ID
     */
    @Transactional
    public void deletePet(UserPrincipal userPrincipal, UUID petId) {
        Pet pet = findPetWithOwnershipCheck(userPrincipal.id(), petId);
        petRepository.deleteById(petId);
    }

    /**
     * 펫의 상세 정보를 조회합니다. 펫 통계 서비스에서 누적 산책 거리를 조회하여 함께 반환합니다.
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @param petId         조회할 펫의 ID
     * @return 펫 상세 정보 응답 DTO (기본 정보 + 통계 정보 포함)
     */
    public PetDetailResponse getPet(UserPrincipal userPrincipal, UUID petId) {
        Pet pet = findPetWithOwnershipCheck(userPrincipal.id(), petId);
        int walkedDistance = petStatistics.getTotalWalkedDistance(petId); // 누적 산책거리 조회
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
}
