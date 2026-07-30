package org.zerock.puppyrun.pet.controller.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.pet.entity.PetWeightLog;

@Builder
public record PetWeightRecordResponse(
        UUID weightLogId,
        UUID petId,
        Double weight,
        LocalDateTime recordedAt
) {
    public static PetWeightRecordResponse of(PetWeightLog petWeightLog) {
        return PetWeightRecordResponse.builder()
                .weightLogId(petWeightLog.getId())
                .petId(petWeightLog.getPet().getId())
                .weight(petWeightLog.getWeight())
                .recordedAt(petWeightLog.getCreatedAt())
                .build();
    }
}
