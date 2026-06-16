package org.zerock.puppyrun.gamification.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.gamification.controller.response.GamificationResponse;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.statistics.service.PetStatistics;
import org.zerock.puppyrun.tracking.DTO.TotalMemberTracking;
import org.zerock.puppyrun.tracking.DTO.TotalPetStat;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamificationQueryService {
    private final TrackingRepository trackingRepository;
    private final PetRepository petRepository;
    private final PetStatistics petStatistics;

    public GamificationResponse getGamification(UserPrincipal principal) {
        TotalMemberTracking memberStat = trackingRepository.getTotalTrackingSummaryByMemberId(principal.id());
        List<UUID> petIds = petRepository.findPetIdsByMemberId(principal.id());
        List<TotalPetStat> petStats = petStatistics.getTotalPetTrackingSummary(petIds);

        return GamificationResponse.of(memberStat, petStats);
    }
}
