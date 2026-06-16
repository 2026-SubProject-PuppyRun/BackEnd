package org.zerock.puppyrun.tracking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.tracking.DTO.TotalPetStat;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;

public interface PetTrackingRepoCustom {
    int sumTotalDistanceByPetId(UUID petId);

    int sumTotalDurationByPetId(UUID petId);

    List<TotalPetTracking> getTrackingSummaryByPetId(List<UUID> petId, LocalDate startDate, LocalDate endDate);

    List<TotalPetStat> getTotalTrackingSummaryByPetIds(List<UUID> petIds);

    int countTogetherTracking(UUID memberId, LocalDate startDate, LocalDate endDate);

//    TotalPetTracking getTrackingSummaryByPetId(UUID id, LocalDate startDate, LocalDate targetDay);

}
