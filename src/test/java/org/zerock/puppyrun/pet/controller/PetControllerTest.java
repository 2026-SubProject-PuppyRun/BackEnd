package org.zerock.puppyrun.pet.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.zerock.puppyrun.common.auth.security.JwtAuthenticationFilter;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.member.entity.UserRole;
import org.zerock.puppyrun.pet.controller.response.PetProgressResponse;
import org.zerock.puppyrun.pet.controller.response.PetProgressResponse.PetProgress;
import org.zerock.puppyrun.pet.controller.response.PetProgressResponse.PetProgress.TrackingProgress;
import org.zerock.puppyrun.pet.service.PetCommandService;
import org.zerock.puppyrun.pet.service.PetQueryService;

@WebMvcTest(PetController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("펫 API")
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetCommandService petCommandService;

    @MockBean
    private PetQueryService petQueryService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(UUID.randomUUID(), "owner@test.com", UserRole.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("전체 진행도 조회는 펫 진행도 목록을 반환한다")
    void getPetProgressList() throws Exception {
        // given
        UUID petId = UUID.randomUUID();
        when(petQueryService.getPetProgress(principal, null))
                .thenReturn(response(petId));

        // when & then
        mockMvc.perform(get("/api/pets/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_progresses[0].pet_id").value(petId.toString()))
                .andExpect(jsonPath("$.pet_progresses[0].tracking_progress.code").value("001"));

        verify(petQueryService).getPetProgress(principal, null);
    }

    @Test
    @DisplayName("펫 ID 하나를 전달하면 한 항목을 담은 진행도 목록을 반환한다")
    void getSinglePetProgress() throws Exception {
        // given
        UUID petId = UUID.randomUUID();
        when(petQueryService.getPetProgress(principal, List.of(petId))).thenReturn(response(petId));

        // when & then
        mockMvc.perform(get("/api/pets/progress").param("petIds", petId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_progresses.length()").value(1))
                .andExpect(jsonPath("$.pet_progresses[0].pet_id").value(petId.toString()))
                .andExpect(jsonPath("$.pet_progresses[0].name").value("몽이"))
                .andExpect(jsonPath("$.pet_progresses[0].tracking_progress.walked_distance").value(12_000));

        verify(petQueryService).getPetProgress(principal, List.of(petId));
    }

    @Test
    @DisplayName("펫 ID 여러 개를 전달하면 해당 펫들의 진행도 목록을 반환한다")
    void getMultiplePetProgresses() throws Exception {
        // given
        UUID firstPetId = UUID.randomUUID();
        UUID secondPetId = UUID.randomUUID();
        List<UUID> petIds = List.of(firstPetId, secondPetId);
        when(petQueryService.getPetProgress(principal, petIds))
                .thenReturn(response(firstPetId, secondPetId));

        // when & then
        mockMvc.perform(get("/api/pets/progress")
                        .param("petIds", firstPetId.toString(), secondPetId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_progresses.length()").value(2))
                .andExpect(jsonPath("$.pet_progresses[0].pet_id").value(firstPetId.toString()))
                .andExpect(jsonPath("$.pet_progresses[1].pet_id").value(secondPetId.toString()));

        verify(petQueryService).getPetProgress(principal, petIds);
    }

    private PetProgressResponse response(UUID... petIds) {
        List<PetProgress> petProgresses = java.util.Arrays.stream(petIds)
                .map(petId -> new PetProgress(
                        petId,
                        "몽이",
                        null,
                        new TrackingProgress("001", 12_000, 10_000, 50_000)
                ))
                .toList();

        return new PetProgressResponse(petProgresses);
    }
}
