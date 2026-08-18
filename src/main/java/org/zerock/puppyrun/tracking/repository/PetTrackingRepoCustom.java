package org.zerock.puppyrun.tracking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;
import org.zerock.puppyrun.tracking.DTO.PetWalkedDistance;

public interface PetTrackingRepoCustom {
    int sumTotalDistanceByPetId(UUID petId);

    List<PetWalkedDistance> findTotalWalkedDistancesByPetIds(List<UUID> petIds);

    int sumTotalDurationByPetId(UUID petId);

    List<TotalPetTracking> getTrackingSummaryByPetId(List<UUID> petId, LocalDate startDate, LocalDate endDate);

    int countTogetherTracking(UUID memberId, LocalDate startDate, LocalDate endDate);

//    TotalPetTracking getTrackingSummaryByPetId(UUID id, LocalDate startDate, LocalDate targetDay);

}
