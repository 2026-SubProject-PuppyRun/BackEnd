package org.zerock.puppyrun.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.member.entity.UserRole;
import org.zerock.puppyrun.pet.controller.response.PetProgressResponse;
import org.zerock.puppyrun.pet.controller.response.PetProgressResponse.PetProgress;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.statistics.service.PetStatistics;

@ExtendWith(MockitoExtension.class)
@DisplayName("펫 진행도 조회 서비스")
class PetQueryServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetStatistics petStatistics;

    private PetQueryService petQueryService;

    @BeforeEach
    void setUp() {
        petQueryService = new PetQueryService(petRepository, petStatistics);
    }

    @Test
    @DisplayName("사용자가 소유한 모든 펫의 진행도를 조회한다")
    void getAllOwnedPetProgresses() {
        // given
        UserPrincipal principal = principal();
        Pet firstPet = pet("몽이", 12_000);
        Pet secondPet = pet("초코", 55_000);
        UUID firstPetId = firstPet.getId();
        UUID secondPetId = secondPet.getId();
        when(petRepository.findAllByMemberId(principal.id()))
                .thenReturn(List.of(firstPet, secondPet));
        when(petStatistics.getTotalWalkedDistances(List.of(firstPetId, secondPetId)))
                .thenReturn(Map.of(firstPetId, 12_000, secondPetId, 55_000));

        // when
        PetProgressResponse result = petQueryService.getPetProgress(principal, null);

        // then
        assertThat(result.petProgresses())
                .extracting(PetProgress::name)
                .containsExactly("몽이", "초코");
        assertThat(result.petProgresses())
                .extracting(progress -> progress.trackingProgress().code())
                .containsExactly("001", "002");
        verify(petRepository).findAllByMemberId(principal.id());
    }

    @Test
    @DisplayName("소유한 펫이 없으면 빈 진행도 목록을 반환한다")
    void returnEmptyProgressListWhenNoPetExists() {
        // given
        UserPrincipal principal = principal();
        when(petRepository.findAllByMemberId(principal.id())).thenReturn(List.of());

        // when
        PetProgressResponse result = petQueryService.getPetProgress(principal, List.of());

        // then
        assertThat(result.petProgresses()).isEmpty();
    }

    @Test
    @DisplayName("소유권이 확인된 특정 펫의 진행도를 조회한다")
    void getSingleOwnedPetProgress() {
        // given
        UserPrincipal principal = principal();
        UUID petId = UUID.randomUUID();
        Pet pet = pet(petId, "몽이", 12_000);
        when(petRepository.findByIdAndVerifyOwnership(petId, principal.id())).thenReturn(pet);
        when(petStatistics.getTotalWalkedDistances(List.of(petId))).thenReturn(Map.of(petId, 12_000));

        // when
        PetProgressResponse result = petQueryService.getPetProgress(principal, List.of(petId));

        // then
        assertThat(result.petProgresses()).singleElement().satisfies(progress -> {
            assertThat(progress.petId()).isEqualTo(petId);
            assertThat(progress.name()).isEqualTo("몽이");
            assertThat(progress.trackingProgress().code()).isEqualTo("001");
            assertThat(progress.trackingProgress().walkedDistance()).isEqualTo(12_000);
        });
        verify(petRepository).findByIdAndVerifyOwnership(petId, principal.id());
    }

    @Test
    @DisplayName("여러 펫 ID를 전달하면 요청 순서대로 진행도 목록을 반환한다")
    void getMultipleOwnedPetProgressesInRequestedOrder() {
        // given
        UserPrincipal principal = principal();
        Pet firstPet = pet("몽이", 12_000);
        Pet secondPet = pet("초코", 55_000);
        UUID firstPetId = firstPet.getId();
        UUID secondPetId = secondPet.getId();
        List<UUID> requestedIds = List.of(firstPetId, secondPetId);
        when(petRepository.findAllByMemberIdAndIdIn(principal.id(), requestedIds))
                .thenReturn(List.of(secondPet, firstPet));
        when(petStatistics.getTotalWalkedDistances(requestedIds))
                .thenReturn(Map.of(firstPetId, 12_000, secondPetId, 55_000));

        // when
        PetProgressResponse result = petQueryService.getPetProgress(principal, requestedIds);

        // then
        assertThat(result.petProgresses())
                .extracting(PetProgress::petId)
                .containsExactlyElementsOf(requestedIds);
    }

    @Test
    @DisplayName("여러 펫 중 조회할 수 없는 ID가 포함되면 부분 목록을 반환하지 않는다")
    void rejectMultipleProgressLookupWhenPetIsUnavailable() {
        // given
        UserPrincipal principal = principal();
        UUID ownedPetId = UUID.randomUUID();
        Pet ownedPet = mock(Pet.class);
        when(ownedPet.getId()).thenReturn(ownedPetId);
        List<UUID> requestedIds = List.of(ownedPetId, UUID.randomUUID());
        when(petRepository.findAllByMemberIdAndIdIn(principal.id(), requestedIds))
                .thenReturn(List.of(ownedPet));

        // when & then
        assertThatThrownBy(() -> petQueryService.getPetProgress(principal, requestedIds))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("조회할 수 없는 펫");
    }

    private UserPrincipal principal() {
        return new UserPrincipal(UUID.randomUUID(), "owner@test.com", UserRole.USER);
    }

    private Pet pet(String name, int walkedDistance) {
        return pet(UUID.randomUUID(), name, walkedDistance);
    }

    private Pet pet(UUID petId, String name, int walkedDistance) {
        Pet pet = mock(Pet.class);
        when(pet.getId()).thenReturn(petId);
        when(pet.getName()).thenReturn(name);
        return pet;
    }
}
