package org.zerock.puppyrun.pet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.common.exception.ErrorCode;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.pet.controller.request.RegisterPetWeightLogRequest;
import org.zerock.puppyrun.pet.entity.Breed;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetWeightLog;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.pet.repository.PetWeightLogRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PetWeightLogRepository petWeightLogRepository;

    @BeforeEach
    void setUp() {
        petWeightLogRepository.deleteAll();
        petRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("체중 기록 등록 API")
    void registerPetWeightLog() throws Exception {
        Member member = createMember("register-weight@test.com", "registerWeightUser");
        Pet pet = createPet(member, "몽이", 3.4);
        String accessToken = createAccessToken(member);

        RegisterPetWeightLogRequest request = new RegisterPetWeightLogRequest(4.2);

        mockMvc.perform(post("/api/pets/{petId}/weight-logs", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight_log_id").exists())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.weight").value(4.2))
                .andExpect(jsonPath("$.recorded_at").exists());

        Pet updatedPet = petRepository.findById(pet.getId()).orElseThrow();
        assertThat(updatedPet.getWeight()).isEqualTo(4.2);

        assertThat(petWeightLogRepository.count()).isEqualTo(1);
        PetWeightLog savedLog = petWeightLogRepository.findAll().getFirst();
        assertThat(savedLog.getPet().getId()).isEqualTo(pet.getId());
        assertThat(savedLog.getWeight()).isEqualTo(4.2);
    }

    @Test
    @DisplayName("존재하지 않는 펫으로 체중 기록 등록 요청 시 404를 반환한다")
    void registerPetWeightLog_whenPetNotFound() throws Exception {
        Member member = createMember("pet-not-found-weight@test.com", "petNotFoundWeightUser");
        String accessToken = createAccessToken(member);
        UUID notFoundPetId = UUID.randomUUID();

        RegisterPetWeightLogRequest request = new RegisterPetWeightLogRequest(4.2);

        mockMvc.perform(post("/api/pets/{petId}/weight-logs", notFoundPetId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + notFoundPetId + "/weight-logs"));

        assertThat(petWeightLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("다른 사용자의 펫에 체중 기록 요청 시 403을 반환한다")
    void registerPetWeightLog_whenPetOwnedByOtherUser() throws Exception {
        Member owner = createMember("weight-owner@test.com", "weightOwnerUser");
        Member stranger = createMember("weight-stranger@test.com", "weightStrangerUser");
        Pet ownerPet = createPet(owner, "주인강아지", 3.4);
        PetWeightLog existingLog = createWeightLog(ownerPet, 3.4);
        String strangerAccessToken = createAccessToken(stranger);

        RegisterPetWeightLogRequest request = new RegisterPetWeightLogRequest(4.2);

        mockMvc.perform(post("/api/pets/{petId}/weight-logs", ownerPet.getId())
                        .header("Authorization", "Bearer " + strangerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫에 대한 권한이 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + ownerPet.getId() + "/weight-logs"));

        Pet notUpdatedPet = petRepository.findById(ownerPet.getId()).orElseThrow();
        PetWeightLog notUpdatedLog = petWeightLogRepository.findById(existingLog.getId()).orElseThrow();
        assertThat(notUpdatedPet.getWeight()).isEqualTo(3.4);
        assertThat(notUpdatedLog.getWeight()).isEqualTo(3.4);
        assertThat(petWeightLogRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("인증 없이 체중 기록 등록 요청 시 401을 반환한다")
    void registerPetWeightLog_whenUnauthenticated() throws Exception {
        Member member = createMember("unauthenticated-weight@test.com", "unauthenticatedWeightUser");
        Pet pet = createPet(member, "나비", 3.4);

        RegisterPetWeightLogRequest request = new RegisterPetWeightLogRequest(4.2);

        mockMvc.perform(post("/api/pets/{petId}/weight-logs", pet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.MISSING_AUTHORIZATION_HEADER.getCode()))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/weight-logs"));

        Pet notUpdatedPet = petRepository.findById(pet.getId()).orElseThrow();
        assertThat(notUpdatedPet.getWeight()).isEqualTo(3.4);
        assertThat(petWeightLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 체중 기록 등록 요청 시 401을 반환한다")
    void registerPetWeightLog_whenInvalidToken() throws Exception {
        Member member = createMember("invalid-token-weight@test.com", "invalidTokenWeightUser");
        Pet pet = createPet(member, "구름", 3.4);

        RegisterPetWeightLogRequest request = new RegisterPetWeightLogRequest(4.2);

        mockMvc.perform(post("/api/pets/{petId}/weight-logs", pet.getId())
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN.getCode()))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/weight-logs"));

        Pet notUpdatedPet = petRepository.findById(pet.getId()).orElseThrow();
        assertThat(notUpdatedPet.getWeight()).isEqualTo(3.4);
        assertThat(petWeightLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("체중이 0 이하이면 등록에 실패한다")
    void registerPetWeightLog_whenWeightIsInvalid() throws Exception {
        Member member = createMember("invalid-weight@test.com", "invalidWeightUser");
        Pet pet = createPet(member, "토리", 3.4);
        String accessToken = createAccessToken(member);

        RegisterPetWeightLogRequest request = new RegisterPetWeightLogRequest(0.0);

        mockMvc.perform(post("/api/pets/{petId}/weight-logs", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("몸무게는 1kg 이상이어야 합니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/weight-logs"));

        Pet notUpdatedPet = petRepository.findById(pet.getId()).orElseThrow();
        assertThat(notUpdatedPet.getWeight()).isEqualTo(3.4);
        assertThat(petWeightLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("같은 날 이미 체중 기록이 있으면 새 로그를 만들지 않고 기존 로그를 수정한다")
    void registerPetWeightLog_whenSameDayLogExists() throws Exception {
        Member member = createMember("same-day-weight@test.com", "sameDayWeightUser");
        Pet pet = createPet(member, "보리", 3.4);
        PetWeightLog existingLog = createWeightLog(pet, 3.4);
        String accessToken = createAccessToken(member);

        RegisterPetWeightLogRequest request = new RegisterPetWeightLogRequest(4.1);

        mockMvc.perform(post("/api/pets/{petId}/weight-logs", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight_log_id").value(existingLog.getId().toString()))
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.weight").value(4.1))
                .andExpect(jsonPath("$.recorded_at").exists());

        Pet updatedPet = petRepository.findById(pet.getId()).orElseThrow();
        PetWeightLog updatedLog = petWeightLogRepository.findById(existingLog.getId()).orElseThrow();
        assertThat(updatedPet.getWeight()).isEqualTo(4.1);
        assertThat(updatedLog.getWeight()).isEqualTo(4.1);
        assertThat(petWeightLogRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("직전 로그와 같은 체중이면 중복 로그를 추가하지 않는다")
    void registerPetWeightLog_whenWeightIsSameAsLatestLog() throws Exception {
        Member member = createMember("duplicate-weight@test.com", "duplicateWeightUser");
        Pet pet = createPet(member, "하늘", 3.4);
        PetWeightLog existingLog = createWeightLog(pet, 3.4);
        String accessToken = createAccessToken(member);

        RegisterPetWeightLogRequest request = new RegisterPetWeightLogRequest(3.4);

        mockMvc.perform(post("/api/pets/{petId}/weight-logs", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight_log_id").value(existingLog.getId().toString()))
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.weight").value(3.4))
                .andExpect(jsonPath("$.recorded_at").exists());

        Pet notChangedPet = petRepository.findById(pet.getId()).orElseThrow();
        PetWeightLog notChangedLog = petWeightLogRepository.findById(existingLog.getId()).orElseThrow();
        assertThat(notChangedPet.getWeight()).isEqualTo(3.4);
        assertThat(notChangedLog.getWeight()).isEqualTo(3.4);
        assertThat(petWeightLogRepository.count()).isEqualTo(1);
    }

    private Member createMember(String email, String nickname) {
        Member member = Member.builder()
                .email(email)
                .nickName(nickname)
                .password("password")
                .build();
        return memberRepository.save(member);
    }

    private Pet createPet(Member member, String name, double weight) {
        Pet pet = Pet.builder()
                .member(member)
                .name(name)
                .birthYear(LocalDate.of(2022, 1, 1))
                .breed(Breed.MALTESE)
                .color(null)
                .weight(weight)
                .isNeutered(true)
                .gender("M")
                .build();
        return petRepository.save(pet);
    }

    private PetWeightLog createWeightLog(Pet pet, double weight) {
        PetWeightLog petWeightLog = PetWeightLog.builder()
                .pet(pet)
                .weight(weight)
                .build();
        return petWeightLogRepository.save(petWeightLog);
    }

    private String createAccessToken(Member member) {
        return jwtTokenProvider.generateAccessToken(member.toDto());
    }
}
