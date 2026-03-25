package org.zerock.puppyrun.statistics.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetWeightLog;
import org.zerock.puppyrun.pet.repository.PetWeightLogRepository;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;
import org.zerock.puppyrun.tracking.repository.PetTrackingRepository;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PetStatistics {
    private final PetWeightLogRepository petWeightLogRepository;
    private final PetTrackingRepository petTrackingRepository;

    /**
     * 특정 펫의 몸무게를 저장 합니다
     */
    @Transactional
    public void savePetWeightLog(Pet pet, double newWeight) {
        // 최근 로그가 오늘 작성된 것이라면 새로 만들지 않고 값만 수정
        Optional<PetWeightLog> latestLogOpt = petWeightLogRepository.findFirstByPetIdOrderByCreatedAtDesc(pet.getId());

        if (latestLogOpt.isPresent()) {
            PetWeightLog latestLog = latestLogOpt.get();
            // 가장 최근 몸무게와 완벽히 동일하면 아무 작업도 하지 않음
            if (latestLog.getWeight() == newWeight) {
                log.info("이전 몸무게와 동일하여 로그 저장을 스킵합니다. PetID: {}", pet.getId());
                return;
            }

            LocalDate latestLogDate = latestLog.getCreatedAt().toLocalDate();
            if (latestLogDate.equals(LocalDate.now())) {
                latestLog.updateWeight(newWeight);
                log.info("오늘 기록된 로그가 있어 몸무게를 업데이트합니다. PetID: {}", pet.getId());
                return;
            }
        }

        // 위 조건에 해당하지 않으면 기존 몸무게로 새로운 로그 생성
        createNewWeightLog(pet, newWeight);
    }

    /**
     * 펫의 몸무게를 최신순으로 조회합니다.
     *
     * @param petId 조회할 펫의 ID
     */
    public List<PetWeightLog> getPetWeightLog(UUID petId) {
        return petWeightLogRepository.findAllByPetIdOrderByCreatedAt(petId);
    }

    /**
     * 새로운 몸무게 로그를 생성하여 저장합니다.
     *
     * @param pet       로그를 남길 펫
     * @param newWeight 새로운 몸무게
     */
    private void createNewWeightLog(Pet pet, double newWeight) {
        PetWeightLog petWeightLog = PetWeightLog.builder()
                .pet(pet)
                .weight(newWeight)
                .build();
        petWeightLogRepository.save(petWeightLog);
        log.info("새로운 몸무게 로그를 생성합니다. PetID: {}", pet.getId());
    }

    /**
     * 특정 펫의 누적 산책 거리(미터)를 조회합니다.
     *
     * @param pet 펫 엔티티
     */
    public int getTotalWalkedDistance(Pet pet) {
        return petTrackingRepository.sumTotalDistanceByPetId(pet.getId());
    }

    /**
     * 특정 펫의 누적 산책 시간을 조회합니다.
     *
     * @param pet 펫 엔티티
     */
    public int getTotalWalkedDuration(Pet pet) {
        return petTrackingRepository.sumTotalDurationByPetId(pet.getId());
    }

    /**
     * 펫의 요약된 통계를 조회합니다.
     *
     * @param pet 펫 엔티티
     * @return PetTrackingSummary
     */
    public List<TotalPetTracking> getWeeklyPetTrackingSummary(List<Pet> pet, LocalDate targetDay) {
        LocalDate startDate = targetDay.minusDays(6);
        List<UUID> petId = pet.stream().map(Pet::getId).toList();

        return petTrackingRepository.getTrackingSummaryByPetId(petId, startDate, targetDay);
    }


}
