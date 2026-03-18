package org.zerock.puppyrun.pet.controller.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetWeightLog;

@Builder
public record PetWeightLogResponse(
        UUID petId,
        String color,
        Double currentWeight,
        List<WeightLog> weightLogList
) {
    public record WeightLog(
            Double weight,
            LocalDateTime recordedAt
    ) {
        public static WeightLog of(PetWeightLog petWeightLog) {
            return new WeightLog(
                    petWeightLog.getWeight(),
                    petWeightLog.getCreatedAt()
            );
        }

    }

    public static PetWeightLogResponse of(Pet pet, List<PetWeightLog> weightLogList) {
        List<WeightLog> weightLogs = weightLogList.stream()
                .map(WeightLog::of)
                .toList();
        return PetWeightLogResponse.builder()
                .petId(pet.getId())
                .color(pet.getColor())
                .currentWeight(pet.getWeight())
                .weightLogList(weightLogs)
                .build();
    }
}
