package org.zerock.puppyrun.care.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.zerock.puppyrun.care.controller.request.RegisterMedicationRequest;
import org.zerock.puppyrun.care.controller.request.UpdateMedicationRequest;
import org.zerock.puppyrun.care.entity.MedicationRecord;
import org.zerock.puppyrun.care.repository.MedicationRecordRepository;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.common.exception.ErrorCode;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.pet.entity.Breed;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MedicationControllerTest {

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
    private MedicationRecordRepository medicationRecordRepository;

    @BeforeEach
    void setUp() {
        medicationRecordRepository.deleteAll();
        petRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("투약 기록 등록 API")
    void registerMedication() throws Exception {
        Member member = createMember("register-medication@test.com", "registerMedicationUser");
        Pet pet = createPet(member, "몽이");
        String accessToken = createAccessToken(member);

        RegisterMedicationRequest request = new RegisterMedicationRequest(
                "심장사상충 예방약",
                LocalDateTime.of(2026, 4, 20, 9, 0, 0),
                1.0,
                "tablet",
                "식후 투약"
        );

        mockMvc.perform(post("/api/pets/{petId}/medication-logs", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.medication_name").value("심장사상충 예방약"))
                .andExpect(jsonPath("$.administered_at").value("2026-04-20T09:00:00"))
                .andExpect(jsonPath("$.dose_amount").value(1.0))
                .andExpect(jsonPath("$.dose_unit").value("tablet"))
                .andExpect(jsonPath("$.memo").value("식후 투약"));

        assertThat(medicationRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("투약 기록 목록 조회 API")
    void getMedicationList() throws Exception {
        Member member = createMember("list-medication@test.com", "listMedicationUser");
        Pet pet = createPet(member, "두부");
        String accessToken = createAccessToken(member);

        LocalDateTime sameAdministeredAt = LocalDateTime.of(2026, 4, 20, 9, 0, 0);

        medicationRecordRepository.save(MedicationRecord.builder()
                .pet(pet)
                .medicationName("항생제")
                .administeredAt(LocalDateTime.of(2026, 4, 19, 20, 30, 0))
                .doseAmount(0.5)
                .doseUnit("ml")
                .memo(null)
                .build());

        MedicationRecord olderCreatedRecord = medicationRecordRepository.save(MedicationRecord.builder()
                .pet(pet)
                .medicationName("영양제")
                .administeredAt(sameAdministeredAt)
                .doseAmount(1.0)
                .doseUnit("tablet")
                .memo("먼저 저장됨")
                .build());

        Thread.sleep(10);

        MedicationRecord latestCreatedRecord = medicationRecordRepository.save(MedicationRecord.builder()
                .pet(pet)
                .medicationName("심장사상충 예방약")
                .administeredAt(sameAdministeredAt)
                .doseAmount(1.0)
                .doseUnit("tablet")
                .memo("나중에 저장됨")
                .build());

        mockMvc.perform(get("/api/pets/{petId}/medication-logs", pet.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.total_medication_count").value(3))
                .andExpect(jsonPath("$.medication_log_list[0].medication_log_id")
                        .value(latestCreatedRecord.getId().toString()))
                .andExpect(jsonPath("$.medication_log_list[0].medication_name").value("심장사상충 예방약"))
                .andExpect(jsonPath("$.medication_log_list[0].administered_at").value("2026-04-20T09:00:00"))
                .andExpect(jsonPath("$.medication_log_list[1].medication_log_id")
                        .value(olderCreatedRecord.getId().toString()))
                .andExpect(jsonPath("$.medication_log_list[1].medication_name").value("영양제"))
                .andExpect(jsonPath("$.medication_log_list[2].medication_name").value("항생제"))
                .andExpect(jsonPath("$.medication_log_list[2].administered_at").value("2026-04-19T20:30:00"));
    }

    @Test
    @DisplayName("투약 기록이 없으면 빈 목록을 반환한다")
    void getMedicationList_whenEmpty() throws Exception {
        Member member = createMember("empty-list-medication@test.com", "emptyListMedicationUser");
        Pet pet = createPet(member, "초코");
        String accessToken = createAccessToken(member);

        mockMvc.perform(get("/api/pets/{petId}/medication-logs", pet.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.total_medication_count").value(0))
                .andExpect(jsonPath("$.medication_log_list").isArray())
                .andExpect(jsonPath("$.medication_log_list").isEmpty());

        assertThat(medicationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("투약 기록 수정 API")
    void updateMedication() throws Exception {
        Member member = createMember("update-medication@test.com", "updateMedicationUser");
        Pet pet = createPet(member, "보리");
        String accessToken = createAccessToken(member);

        MedicationRecord medicationRecord = medicationRecordRepository.save(MedicationRecord.builder()
                .pet(pet)
                .medicationName("항생제")
                .administeredAt(LocalDateTime.of(2026, 4, 20, 8, 30, 0))
                .doseAmount(0.5)
                .doseUnit("ml")
                .memo("초기 메모")
                .build());

        UpdateMedicationRequest request = new UpdateMedicationRequest(
                "심장사상충 예방약",
                LocalDateTime.of(2026, 4, 20, 9, 0, 0),
                1.0,
                "tablet",
                "식후 투약으로 변경"
        );

        mockMvc.perform(put("/api/pets/{petId}/medication-logs/{medicationLogId}", pet.getId(), medicationRecord.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medication_log_id").value(medicationRecord.getId().toString()))
                .andExpect(jsonPath("$.medication_name").value("심장사상충 예방약"))
                .andExpect(jsonPath("$.administered_at").value("2026-04-20T09:00:00"))
                .andExpect(jsonPath("$.dose_amount").value(1.0))
                .andExpect(jsonPath("$.dose_unit").value("tablet"))
                .andExpect(jsonPath("$.memo").value("식후 투약으로 변경"));

        MedicationRecord updated = medicationRecordRepository.findById(medicationRecord.getId()).orElseThrow();
        assertThat(updated.getMedicationName()).isEqualTo("심장사상충 예방약");
        assertThat(updated.getAdministeredAt()).isEqualTo(LocalDateTime.of(2026, 4, 20, 9, 0, 0));
        assertThat(updated.getDoseAmount()).isEqualTo(1.0);
        assertThat(updated.getDoseUnit()).isEqualTo("tablet");
        assertThat(updated.getMemo()).isEqualTo("식후 투약으로 변경");
    }

    @Test
    @DisplayName("투약 기록 삭제 API")
    void deleteMedication() throws Exception {
        Member member = createMember("delete-medication@test.com", "deleteMedicationUser");
        Pet pet = createPet(member, "해피");
        String accessToken = createAccessToken(member);

        MedicationRecord medicationRecord = medicationRecordRepository.save(MedicationRecord.builder()
                .pet(pet)
                .medicationName("항생제")
                .administeredAt(LocalDateTime.of(2026, 4, 20, 20, 0, 0))
                .doseAmount(0.5)
                .doseUnit("ml")
                .memo("삭제 대상")
                .build());

        mockMvc.perform(delete("/api/pets/{petId}/medication-logs/{medicationLogId}", pet.getId(), medicationRecord.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        assertThat(medicationRecordRepository.findById(medicationRecord.getId())).isEmpty();
        assertThat(medicationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 펫으로 투약 기록 등록 요청 시 404를 반환한다")
    void registerMedication_whenPetNotFound() throws Exception {
        Member member = createMember("pet-not-found-medication@test.com", "petNotFoundMedicationUser");
        String accessToken = createAccessToken(member);
        UUID notFoundPetId = UUID.randomUUID();

        RegisterMedicationRequest request = new RegisterMedicationRequest(
                "심장사상충 예방약",
                LocalDateTime.of(2026, 4, 20, 9, 0, 0),
                1.0,
                "tablet",
                "등록 실패 케이스"
        );

        mockMvc.perform(post("/api/pets/{petId}/medication-logs", notFoundPetId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + notFoundPetId + "/medication-logs"));

        assertThat(medicationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 투약 기록 수정 요청 시 404를 반환한다")
    void updateMedication_whenMedicationNotFound() throws Exception {
        Member member = createMember("medication-not-found@test.com", "medicationNotFoundUser");
        Pet pet = createPet(member, "콩이");
        String accessToken = createAccessToken(member);
        UUID notFoundMedicationLogId = UUID.randomUUID();

        UpdateMedicationRequest request = new UpdateMedicationRequest(
                "심장사상충 예방약",
                LocalDateTime.of(2026, 4, 20, 9, 0, 0),
                1.0,
                "tablet",
                "수정 시도"
        );

        mockMvc.perform(put("/api/pets/{petId}/medication-logs/{medicationLogId}", pet.getId(), notFoundMedicationLogId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("해당 투약 기록을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/pets/" + pet.getId() + "/medication-logs/" + notFoundMedicationLogId));

        assertThat(medicationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("투약량이 0 이하이면 수정에 실패한다")
    void updateMedication_whenDoseAmountIsInvalid() throws Exception {
        Member member = createMember("invalid-update-dose@test.com", "invalidUpdateDoseUser");
        Pet pet = createPet(member, "보리");
        String accessToken = createAccessToken(member);

        MedicationRecord medicationRecord = medicationRecordRepository.save(MedicationRecord.builder()
                .pet(pet)
                .medicationName("항생제")
                .administeredAt(LocalDateTime.of(2026, 4, 20, 8, 30, 0))
                .doseAmount(0.5)
                .doseUnit("ml")
                .memo("초기 메모")
                .build());

        UpdateMedicationRequest request = new UpdateMedicationRequest(
                "심장사상충 예방약",
                LocalDateTime.of(2026, 4, 20, 9, 0, 0),
                0.0,
                "tablet",
                "잘못된 투약량으로 수정"
        );

        mockMvc.perform(put("/api/pets/{petId}/medication-logs/{medicationLogId}", pet.getId(), medicationRecord.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("투약량은 0보다 커야 합니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/pets/" + pet.getId() + "/medication-logs/" + medicationRecord.getId()));

        MedicationRecord notUpdated = medicationRecordRepository.findById(medicationRecord.getId()).orElseThrow();
        assertThat(notUpdated.getMedicationName()).isEqualTo("항생제");
        assertThat(notUpdated.getDoseAmount()).isEqualTo(0.5);
        assertThat(notUpdated.getDoseUnit()).isEqualTo("ml");
    }

    @Test
    @DisplayName("다른 사용자의 펫 투약 기록 수정 요청 시 403을 반환한다")
    void updateMedication_whenPetOwnedByOtherUser() throws Exception {
        Member owner = createMember("medication-update-owner@test.com", "medicationUpdateOwnerUser");
        Member stranger = createMember("medication-update-stranger@test.com", "medicationUpdateStrangerUser");
        Pet ownerPet = createPet(owner, "주인강아지");
        String strangerAccessToken = createAccessToken(stranger);

        MedicationRecord medicationRecord = medicationRecordRepository.save(MedicationRecord.builder()
                .pet(ownerPet)
                .medicationName("항생제")
                .administeredAt(LocalDateTime.of(2026, 4, 20, 9, 0, 0))
                .doseAmount(0.5)
                .doseUnit("ml")
                .memo("권한 테스트")
                .build());

        UpdateMedicationRequest request = new UpdateMedicationRequest(
                "심장사상충 예방약",
                LocalDateTime.of(2026, 4, 20, 10, 0, 0),
                1.0,
                "tablet",
                "타인 수정 시도"
        );

        mockMvc.perform(put("/api/pets/{petId}/medication-logs/{medicationLogId}", ownerPet.getId(), medicationRecord.getId())
                        .header("Authorization", "Bearer " + strangerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫에 대한 권한이 없습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/pets/" + ownerPet.getId() + "/medication-logs/" + medicationRecord.getId()));

        MedicationRecord notUpdated = medicationRecordRepository.findById(medicationRecord.getId()).orElseThrow();
        assertThat(notUpdated.getMedicationName()).isEqualTo("항생제");
        assertThat(notUpdated.getDoseAmount()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("다른 사용자의 펫 투약 기록 조회 요청 시 403을 반환한다")
    void getMedicationList_whenPetOwnedByOtherUser() throws Exception {
        Member owner = createMember("medication-owner@test.com", "medicationOwnerUser");
        Member stranger = createMember("medication-stranger@test.com", "medicationStrangerUser");
        Pet ownerPet = createPet(owner, "주인강아지");
        String strangerAccessToken = createAccessToken(stranger);

        medicationRecordRepository.save(MedicationRecord.builder()
                .pet(ownerPet)
                .medicationName("항생제")
                .administeredAt(LocalDateTime.of(2026, 4, 20, 9, 0, 0))
                .doseAmount(0.5)
                .doseUnit("ml")
                .memo("권한 테스트")
                .build());

        mockMvc.perform(get("/api/pets/{petId}/medication-logs", ownerPet.getId())
                        .header("Authorization", "Bearer " + strangerAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫에 대한 권한이 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + ownerPet.getId() + "/medication-logs"));

        assertThat(medicationRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("투약량이 0 이하이면 등록에 실패한다")
    void registerMedication_whenDoseAmountIsInvalid() throws Exception {
        Member member = createMember("invalid-dose@test.com", "invalidDoseUser");
        Pet pet = createPet(member, "토리");
        String accessToken = createAccessToken(member);

        RegisterMedicationRequest request = new RegisterMedicationRequest(
                "심장사상충 예방약",
                LocalDateTime.of(2026, 4, 20, 9, 0, 0),
                0.0,
                "tablet",
                "잘못된 투약량"
        );

        mockMvc.perform(post("/api/pets/{petId}/medication-logs", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("투약량은 0보다 커야 합니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/medication-logs"));

        assertThat(medicationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 투약 기록 삭제 요청 시 404를 반환한다")
    void deleteMedication_whenMedicationNotFound() throws Exception {
        Member member = createMember("delete-not-found-medication@test.com", "deleteNotFoundMedicationUser");
        Pet pet = createPet(member, "까미");
        String accessToken = createAccessToken(member);
        UUID notFoundMedicationLogId = UUID.randomUUID();

        mockMvc.perform(delete("/api/pets/{petId}/medication-logs/{medicationLogId}", pet.getId(), notFoundMedicationLogId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("해당 투약 기록을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/pets/" + pet.getId() + "/medication-logs/" + notFoundMedicationLogId));

        assertThat(medicationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("인증 없이 투약 기록 등록 요청 시 401을 반환한다")
    void registerMedication_whenUnauthenticated() throws Exception {
        Member member = createMember("unauthenticated@test.com", "unauthenticatedUser");
        Pet pet = createPet(member, "나비");

        RegisterMedicationRequest request = new RegisterMedicationRequest(
                "심장사상충 예방약",
                LocalDateTime.of(2026, 4, 20, 9, 0, 0),
                1.0,
                "tablet",
                "인증 없음"
        );

        mockMvc.perform(post("/api/pets/{petId}/medication-logs", pet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.MISSING_AUTHORIZATION_HEADER.getCode()))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/medication-logs"));

        assertThat(medicationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 투약 기록 등록 요청 시 401을 반환한다")
    void registerMedication_whenInvalidToken() throws Exception {
        Member member = createMember("invalid-token@test.com", "invalidTokenUser");
        Pet pet = createPet(member, "구름");

        RegisterMedicationRequest request = new RegisterMedicationRequest(
                "심장사상충 예방약",
                LocalDateTime.of(2026, 4, 20, 9, 0, 0),
                1.0,
                "tablet",
                "잘못된 토큰"
        );

        mockMvc.perform(post("/api/pets/{petId}/medication-logs", pet.getId())
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN.getCode()))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/medication-logs"));

        assertThat(medicationRecordRepository.count()).isZero();
    }

    private Member createMember(String email, String nickname) {
        Member member = Member.builder()
                .email(email)
                .nickName(nickname)
                .password("password")
                .build();
        return memberRepository.save(member);
    }

    private Pet createPet(Member member, String name) {
        Pet pet = Pet.builder()
                .member(member)
                .name(name)
                .birthYear(LocalDate.of(2022, 1, 1))
                .breed(Breed.MALTESE)
                .color(null)
                .weight(3.4)
                .isNeutered(true)
                .gender("M")
                .build();
        return petRepository.save(pet);
    }

    private String createAccessToken(Member member) {
        return jwtTokenProvider.generateAccessToken(member.toDto());
    }
}
