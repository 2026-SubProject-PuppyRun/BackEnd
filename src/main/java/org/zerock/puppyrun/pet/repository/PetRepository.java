package org.zerock.puppyrun.pet.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.pet.entity.Pet;

@Repository
public interface PetRepository extends JpaRepository<Pet, UUID> {
    List<Pet> findAllByMemberId(UUID memberId);
}
