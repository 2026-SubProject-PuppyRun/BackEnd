package org.zerock.puppyrun.pet.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.pet.entity.Pet;

@Repository
public interface PetRepository extends JpaRepository<Pet, UUID> {
    /**
     * 펫을 조회하고, 요청한 사용자의 소유인지 검증합니다.
     *
     * @param petId    조회할 펫 ID
     * @param memberId 요청한 사용자의 ID
     * @return 검증이 완료된 Pet 엔티티
     */
    default Pet findByIdAndVerifyOwnership(UUID petId, UUID memberId) {
        Pet pet = findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 펫을 찾을 수 없습니다."));

        if (pet.isNotOwner(memberId)) {
            throw new UserForbiddenException("해당 펫에 대한 권한이 없습니다.");
        }

        return pet;
    }

    List<Pet> findAllByMemberId(UUID memberId);

    @Query("SELECT p.id FROM Pet p WHERE p.member.id = :memberId")
    List<UUID> findPetIdsByMemberId(@Param("memberId") UUID memberId);
}
