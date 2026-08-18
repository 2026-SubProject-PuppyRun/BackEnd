package org.zerock.puppyrun.pet.controller.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetBadge;

public record PetProgressResponse(
        List<PetProgress> petProgresses
) {

    public static PetProgressResponse from(List<Pet> pets, Map<UUID, Integer> walkedDistances) {
        List<PetProgress> petProgresses = pets.stream()
                .map(pet -> PetProgress.from(pet, walkedDistances.getOrDefault(pet.getId(), 0)))
                .toList();

        return new PetProgressResponse(petProgresses);
    }

    @Builder
    public record PetProgress(
            UUID petId,
            String name,
            @S3Url String profileImage,
            TrackingProgress trackingProgress
    ) {

        public static PetProgress from(Pet pet, int walkedDistance) {
            return PetProgress.builder()
                    .petId(pet.getId())
                    .name(pet.getName())
                    .profileImage(pet.getProfileImageUrl())
                    .trackingProgress(TrackingProgress.from(walkedDistance))
                    .build();
        }

        @Builder
        public record TrackingProgress(
                String code,
                int walkedDistance,
                int requiredDistance,
                int nextRequiredDistance
        ) {

            public static TrackingProgress from(int distance) {
                PetBadge badge = PetBadge.getBadgeByDistance(distance);
                return TrackingProgress.builder()
                        .code(badge.getCode())
                        .walkedDistance(distance)
                        .requiredDistance(badge.getRequiredDistance())
                        .nextRequiredDistance(badge.getNextRequiredDistance())
                        .build();
            }
        }
    }
}
