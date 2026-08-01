package org.zerock.puppyrun.pet.controller.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetBadge;

public record PetProgressResponse(
        List<PetProgress> petProgresses
) {

    public static PetProgressResponse from(List<Pet> pets) {
        List<PetProgress> petProgresses = pets.stream()
                .map(PetProgress::from)
                .toList();

        return new PetProgressResponse(petProgresses);
    }

    public static PetProgressResponse from(Pet pets) {
        List<PetProgress> petProgresses = List.of(PetProgress.from(pets));

        return new PetProgressResponse(petProgresses);
    }

    @Builder
    public record PetProgress(
            UUID petId,
            String name,
            @S3Url String profileImage,
            TrackingProgress trackingProgress
    ) {

        public static PetProgress from(Pet pet) {
            return PetProgress.builder()
                    .petId(pet.getId())
                    .name(pet.getName())
                    .profileImage(pet.getProfileImageUrl())
                    .trackingProgress(TrackingProgress.from(pet.getWalkedDistance()))
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
