package org.zerock.puppyrun.tracking.DTO;

import java.util.UUID;

public record PetWalkedDistance(UUID petId, int walkedDistance) {
}
