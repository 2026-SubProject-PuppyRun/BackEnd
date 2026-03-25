package org.zerock.puppyrun.tracking.repository;


import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.tracking.entity.PetTracking;

@Repository
public interface PetTrackingRepository extends JpaRepository<PetTracking, UUID>, PetTrackingRepoCustom {

    @Query("SELECT pt.pet FROM PetTracking pt WHERE pt.tracking.id = :trackingId")
    List<Pet> findAllPetsByTrackingId(@Param("trackingId") UUID trackingId);
}
