package org.zerock.puppyrun.statistics.controller.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.statistics.DTO.PetActivityTracking;
import org.zerock.puppyrun.tracking.util.PaceConverter;

public record PetActivityResponse(
        List<PetActivity> activities
) {

    public record PetActivity(
            UUID petId,
            String petName,
            @S3Url String petProfileUrl,
            LatestActivity latestActivity
    ) {

        private static PetActivity from(Pet pet, PetActivityTracking activity) {
            return new PetActivity(
                    pet.getId(),
                    pet.getName(),
                    pet.getProfileImageUrl(),
                    LatestActivity.from(activity)
            );
        }
    }

    public record LatestActivity(
            UUID trackingId,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Integer distanceM,
            Integer durationSec,
            String averagePace
    ) {

        private static LatestActivity from(PetActivityTracking activity) {
            if (activity == null) {
                return null;
            }

            return new LatestActivity(
                    activity.trackingId(),
                    activity.startedAt(),
                    activity.endedAt(),
                    activity.distance(),
                    activity.duration(),
                    PaceConverter.toString(activity.averagePace())
            );
        }
    }

    public static PetActivityResponse of(
            List<Pet> petList,
            Map<Pet, PetActivityTracking> latestActivities
    ) {
        List<PetActivity> activities = petList.stream()
                .map(pet -> PetActivity.from(
                        pet,
                        latestActivities.get(pet)
                ))
                .toList();

        return new PetActivityResponse(activities);
    }
}
