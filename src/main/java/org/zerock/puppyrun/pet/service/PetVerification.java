package org.zerock.puppyrun.pet.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;

@Component
@RequiredArgsConstructor
public class PetVerification {
    public final PetRepository petRepository;

    /**
     * 펫 ID로 펫을 조회하고, 요청한 사용자가 소유자인지 검증합니다.
     *
     * @param memberId 요청한 사용자의 ID
     * @param petId    조회할 펫의 ID
     * @return 조회된 Pet 엔티티
     * @throws ResourceNotFoundException 펫이 존재하지 않을 경우
     * @throws UserForbiddenException    사용자가 해당 펫의 소유자가 아닐 경우
     */
    Pet ownershipCheck(UUID memberId, UUID petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("펫을 찾을 수 없습니다."));

        if (pet.isNotOwner(memberId)) {
            throw new UserForbiddenException("해당 펫에 대한 권한이 없습니다.");
        }

        return pet;
    }
}
