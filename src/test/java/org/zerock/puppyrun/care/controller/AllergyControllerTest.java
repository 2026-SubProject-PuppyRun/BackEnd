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
import org.zerock.puppyrun.care.controller.request.RegisterAllergyRequest;
import org.zerock.puppyrun.care.controller.request.UpdateAllergyRequest;
import org.zerock.puppyrun.care.entity.AllergyRecord;
import org.zerock.puppyrun.care.entity.AllergySeverity;
import org.zerock.puppyrun.care.repository.AllergyRecordRepository;
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
class AllergyControllerTest {

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
    private AllergyRecordRepository allergyRecordRepository;

    @BeforeEach
    void setUp() {
        allergyRecordRepository.deleteAll();
        petRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("알러지 기록 등록 API")
    void registerAllergy() throws Exception {
        Member member = createMember("register-allergy@test.com", "registerAllergyUser");
        Pet pet = createPet(member, "몽이");
        String accessToken = createAccessToken(member);

        RegisterAllergyRequest request = new RegisterAllergyRequest(
                "닭고기",
                "피부 가려움",
                "MODERATE",
                LocalDate.of(2026, 4, 20),
                true,
                "간식 섭취 후 반응 확인"
        );

        mockMvc.perform(post("/api/pets/{petId}/allergies", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.allergen_name").value("닭고기"))
                .andExpect(jsonPath("$.symptom").value("피부 가려움"))
                .andExpect(jsonPath("$.severity").value("MODERATE"))
                .andExpect(jsonPath("$.identified_at").value("2026-04-20"))
                .andExpect(jsonPath("$.is_active").value(true))
                .andExpect(jsonPath("$.memo").value("간식 섭취 후 반응 확인"));

        assertThat(allergyRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("알러지 기록 목록 조회 API")
    void getAllergyList() throws Exception {
        Member member = createMember("list-allergy@test.com", "listAllergyUser");
        Pet pet = createPet(member, "두부");
        String accessToken = createAccessToken(member);

        AllergyRecord olderCreatedRecord = allergyRecordRepository.save(AllergyRecord.builder()
                .pet(pet)
                .allergenName("유제품")
                .symptom("설사")
                .severity(AllergySeverity.MILD)
                .identifiedAt(LocalDate.of(2025, 12, 1))
                .isActive(false)
                .memo(null)
                .build());

        Thread.sleep(10);

        AllergyRecord latestCreatedRecord = allergyRecordRepository.save(AllergyRecord.builder()
                .pet(pet)
                .allergenName("닭고기")
                .symptom("피부 가려움")
                .severity(AllergySeverity.MODERATE)
                .identifiedAt(LocalDate.of(2026, 4, 20))
                .isActive(true)
                .memo("간식 섭취 후 반응 확인")
                .build());

        mockMvc.perform(get("/api/pets/{petId}/allergies", pet.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.total_allergy_count").value(2))
                .andExpect(jsonPath("$.allergy_list[0].allergy_id").value(latestCreatedRecord.getId().toString()))
                .andExpect(jsonPath("$.allergy_list[0].allergen_name").value("닭고기"))
                .andExpect(jsonPath("$.allergy_list[0].severity").value("MODERATE"))
                .andExpect(jsonPath("$.allergy_list[1].allergy_id").value(olderCreatedRecord.getId().toString()))
                .andExpect(jsonPath("$.allergy_list[1].allergen_name").value("유제품"))
                .andExpect(jsonPath("$.allergy_list[1].severity").value("MILD"));
    }

    @Test
    @DisplayName("알러지 기록이 없으면 빈 목록을 반환한다")
    void getAllergyList_whenEmpty() throws Exception {
        Member member = createMember("empty-list-allergy@test.com", "emptyListAllergyUser");
        Pet pet = createPet(member, "초코");
        String accessToken = createAccessToken(member);

        mockMvc.perform(get("/api/pets/{petId}/allergies", pet.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.total_allergy_count").value(0))
                .andExpect(jsonPath("$.allergy_list").isArray())
                .andExpect(jsonPath("$.allergy_list").isEmpty());

        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("알러지 기록 수정 API")
    void updateAllergy() throws Exception {
        Member member = createMember("update-allergy@test.com", "updateAllergyUser");
        Pet pet = createPet(member, "보리");
        String accessToken = createAccessToken(member);

        AllergyRecord allergyRecord = allergyRecordRepository.save(AllergyRecord.builder()
                .pet(pet)
                .allergenName("유제품")
                .symptom("설사")
                .severity(AllergySeverity.MILD)
                .identifiedAt(LocalDate.of(2025, 12, 1))
                .isActive(true)
                .memo("초기 메모")
                .build());

        UpdateAllergyRequest request = new UpdateAllergyRequest(
                "닭고기",
                "피부 가려움",
                "SEVERE",
                LocalDate.of(2026, 4, 20),
                false,
                "사료 변경 후 비활성 처리"
        );

        mockMvc.perform(put("/api/pets/{petId}/allergies/{allergyId}", pet.getId(), allergyRecord.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergy_id").value(allergyRecord.getId().toString()))
                .andExpect(jsonPath("$.allergen_name").value("닭고기"))
                .andExpect(jsonPath("$.symptom").value("피부 가려움"))
                .andExpect(jsonPath("$.severity").value("SEVERE"))
                .andExpect(jsonPath("$.identified_at").value("2026-04-20"))
                .andExpect(jsonPath("$.is_active").value(false))
                .andExpect(jsonPath("$.memo").value("사료 변경 후 비활성 처리"));

        AllergyRecord updated = allergyRecordRepository.findById(allergyRecord.getId()).orElseThrow();
        assertThat(updated.getAllergenName()).isEqualTo("닭고기");
        assertThat(updated.getSymptom()).isEqualTo("피부 가려움");
        assertThat(updated.getSeverity()).isEqualTo(AllergySeverity.SEVERE);
        assertThat(updated.getIsActive()).isFalse();
        assertThat(updated.getMemo()).isEqualTo("사료 변경 후 비활성 처리");
    }

    @Test
    @DisplayName("알러지 기록 삭제 API")
    void deleteAllergy() throws Exception {
        Member member = createMember("delete-allergy@test.com", "deleteAllergyUser");
        Pet pet = createPet(member, "해피");
        String accessToken = createAccessToken(member);

        AllergyRecord allergyRecord = allergyRecordRepository.save(AllergyRecord.builder()
                .pet(pet)
                .allergenName("닭고기")
                .symptom("피부 가려움")
                .severity(AllergySeverity.MODERATE)
                .identifiedAt(LocalDate.of(2026, 4, 20))
                .isActive(true)
                .memo("삭제 대상")
                .build());

        mockMvc.perform(delete("/api/pets/{petId}/allergies/{allergyId}", pet.getId(), allergyRecord.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        assertThat(allergyRecordRepository.findById(allergyRecord.getId())).isEmpty();
        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 펫으로 알러지 기록 등록 요청 시 404를 반환한다")
    void registerAllergy_whenPetNotFound() throws Exception {
        Member member = createMember("pet-not-found-allergy@test.com", "petNotFoundAllergyUser");
        String accessToken = createAccessToken(member);
        UUID notFoundPetId = UUID.randomUUID();

        RegisterAllergyRequest request = new RegisterAllergyRequest(
                "닭고기",
                "피부 가려움",
                "MODERATE",
                LocalDate.of(2026, 4, 20),
                true,
                "등록 실패 케이스"
        );

        mockMvc.perform(post("/api/pets/{petId}/allergies", notFoundPetId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + notFoundPetId + "/allergies"));

        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 알러지 기록 수정 요청 시 404를 반환한다")
    void updateAllergy_whenAllergyNotFound() throws Exception {
        Member member = createMember("allergy-not-found@test.com", "allergyNotFoundUser");
        Pet pet = createPet(member, "콩이");
        String accessToken = createAccessToken(member);
        UUID notFoundAllergyId = UUID.randomUUID();

        UpdateAllergyRequest request = new UpdateAllergyRequest(
                "닭고기",
                "피부 가려움",
                "MODERATE",
                LocalDate.of(2026, 4, 20),
                true,
                "수정 시도"
        );

        mockMvc.perform(put("/api/pets/{petId}/allergies/{allergyId}", pet.getId(), notFoundAllergyId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("해당 알러지 기록을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/allergies/" + notFoundAllergyId));

        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("다른 사용자의 펫 알러지 기록 조회 요청 시 403을 반환한다")
    void getAllergyList_whenPetOwnedByOtherUser() throws Exception {
        Member owner = createMember("allergy-owner@test.com", "allergyOwnerUser");
        Member stranger = createMember("allergy-stranger@test.com", "allergyStrangerUser");
        Pet ownerPet = createPet(owner, "주인강아지");
        String strangerAccessToken = createAccessToken(stranger);

        allergyRecordRepository.save(AllergyRecord.builder()
                .pet(ownerPet)
                .allergenName("닭고기")
                .symptom("피부 가려움")
                .severity(AllergySeverity.MODERATE)
                .identifiedAt(LocalDate.of(2026, 4, 20))
                .isActive(true)
                .memo("권한 테스트")
                .build());

        mockMvc.perform(get("/api/pets/{petId}/allergies", ownerPet.getId())
                        .header("Authorization", "Bearer " + strangerAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫에 대한 권한이 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + ownerPet.getId() + "/allergies"));

        assertThat(allergyRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 사용자의 펫 알러지 기록 수정 요청 시 403을 반환한다")
    void updateAllergy_whenPetOwnedByOtherUser() throws Exception {
        Member owner = createMember("allergy-update-owner@test.com", "allergyUpdateOwnerUser");
        Member stranger = createMember("allergy-update-stranger@test.com", "allergyUpdateStrangerUser");
        Pet ownerPet = createPet(owner, "주인강아지");
        String strangerAccessToken = createAccessToken(stranger);

        AllergyRecord allergyRecord = allergyRecordRepository.save(AllergyRecord.builder()
                .pet(ownerPet)
                .allergenName("유제품")
                .symptom("설사")
                .severity(AllergySeverity.MILD)
                .identifiedAt(LocalDate.of(2025, 12, 1))
                .isActive(true)
                .memo("권한 테스트")
                .build());

        UpdateAllergyRequest request = new UpdateAllergyRequest(
                "닭고기",
                "피부 가려움",
                "SEVERE",
                LocalDate.of(2026, 4, 20),
                false,
                "타인 수정 시도"
        );

        mockMvc.perform(put("/api/pets/{petId}/allergies/{allergyId}", ownerPet.getId(), allergyRecord.getId())
                        .header("Authorization", "Bearer " + strangerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫에 대한 권한이 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + ownerPet.getId() + "/allergies/" + allergyRecord.getId()));

        AllergyRecord notUpdated = allergyRecordRepository.findById(allergyRecord.getId()).orElseThrow();
        assertThat(notUpdated.getAllergenName()).isEqualTo("유제품");
        assertThat(notUpdated.getSeverity()).isEqualTo(AllergySeverity.MILD);
    }

    @Test
    @DisplayName("잘못된 알러지 심각도면 등록에 실패한다")
    void registerAllergy_whenSeverityIsInvalid() throws Exception {
        Member member = createMember("invalid-severity@test.com", "invalidSeverityUser");
        Pet pet = createPet(member, "토리");
        String accessToken = createAccessToken(member);

        RegisterAllergyRequest request = new RegisterAllergyRequest(
                "닭고기",
                "피부 가려움",
                "INVALID",
                LocalDate.of(2026, 4, 20),
                true,
                "잘못된 심각도"
        );

        mockMvc.perform(post("/api/pets/{petId}/allergies", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("알러지 심각도 값이 올바르지 않습니다. | INVALID"))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/allergies"));

        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("심각도 값이 없어도 알러지 기록 등록에 성공한다")
    void registerAllergy_whenSeverityIsNull() throws Exception {
        Member member = createMember("null-severity@test.com", "nullSeverityUser");
        Pet pet = createPet(member, "하늘");
        String accessToken = createAccessToken(member);

        RegisterAllergyRequest request = new RegisterAllergyRequest(
                "밀가루",
                "귀 주변 발진",
                null,
                LocalDate.of(2026, 4, 21),
                true,
                "심각도 미정"
        );

        mockMvc.perform(post("/api/pets/{petId}/allergies", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.allergen_name").value("밀가루"))
                .andExpect(jsonPath("$.severity").isEmpty())
                .andExpect(jsonPath("$.is_active").value(true));

        AllergyRecord saved = allergyRecordRepository.findAll().getFirst();
        assertThat(saved.getSeverity()).isNull();
        assertThat(saved.getAllergenName()).isEqualTo("밀가루");
    }

    @Test
    @DisplayName("알러지 원인명이 비어 있으면 등록에 실패한다")
    void registerAllergy_whenAllergenNameIsBlank() throws Exception {
        Member member = createMember("blank-allergen@test.com", "blankAllergenUser");
        Pet pet = createPet(member, "담이");
        String accessToken = createAccessToken(member);

        RegisterAllergyRequest request = new RegisterAllergyRequest(
                "",
                "피부 가려움",
                "MODERATE",
                LocalDate.of(2026, 4, 20),
                true,
                "잘못된 원인명"
        );

        mockMvc.perform(post("/api/pets/{petId}/allergies", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("알러지 원인명은 필수입니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/allergies"));

        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("활성 여부가 없으면 등록에 실패한다")
    void registerAllergy_whenIsActiveIsMissing() throws Exception {
        Member member = createMember("missing-active@test.com", "missingActiveUser");
        Pet pet = createPet(member, "마루");
        String accessToken = createAccessToken(member);

        String requestJson = """
                {
                  "allergen_name": "닭고기",
                  "symptom": "피부 가려움",
                  "severity": "MODERATE",
                  "identified_at": "2026-04-20",
                  "memo": "활성 여부 누락"
                }
                """;

        mockMvc.perform(post("/api/pets/{petId}/allergies", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("활성 여부는 필수입니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/allergies"));

        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("인증 없이 알러지 기록 등록 요청 시 401을 반환한다")
    void registerAllergy_whenUnauthenticated() throws Exception {
        Member member = createMember("unauthenticated-allergy@test.com", "unauthenticatedAllergyUser");
        Pet pet = createPet(member, "나비");

        RegisterAllergyRequest request = new RegisterAllergyRequest(
                "닭고기",
                "피부 가려움",
                "MODERATE",
                LocalDate.of(2026, 4, 20),
                true,
                "인증 없음"
        );

        mockMvc.perform(post("/api/pets/{petId}/allergies", pet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.MISSING_AUTHORIZATION_HEADER.getCode()))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/allergies"));

        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 알러지 기록 등록 요청 시 401을 반환한다")
    void registerAllergy_whenInvalidToken() throws Exception {
        Member member = createMember("invalid-token-allergy@test.com", "invalidTokenAllergyUser");
        Pet pet = createPet(member, "구름");

        RegisterAllergyRequest request = new RegisterAllergyRequest(
                "닭고기",
                "피부 가려움",
                "MODERATE",
                LocalDate.of(2026, 4, 20),
                true,
                "잘못된 토큰"
        );

        mockMvc.perform(post("/api/pets/{petId}/allergies", pet.getId())
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN.getCode()))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/allergies"));

        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 알러지 기록 삭제 요청 시 404를 반환한다")
    void deleteAllergy_whenAllergyNotFound() throws Exception {
        Member member = createMember("delete-not-found-allergy@test.com", "deleteNotFoundAllergyUser");
        Pet pet = createPet(member, "별이");
        String accessToken = createAccessToken(member);
        UUID notFoundAllergyId = UUID.randomUUID();

        mockMvc.perform(delete("/api/pets/{petId}/allergies/{allergyId}", pet.getId(), notFoundAllergyId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("해당 알러지 기록을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/allergies/" + notFoundAllergyId));

        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("다른 사용자의 펫 알러지 기록 삭제 요청 시 403을 반환한다")
    void deleteAllergy_whenPetOwnedByOtherUser() throws Exception {
        Member owner = createMember("allergy-delete-owner@test.com", "allergyDeleteOwnerUser");
        Member stranger = createMember("allergy-delete-stranger@test.com", "allergyDeleteStrangerUser");
        Pet ownerPet = createPet(owner, "주인강아지");
        String strangerAccessToken = createAccessToken(stranger);

        AllergyRecord allergyRecord = allergyRecordRepository.save(AllergyRecord.builder()
                .pet(ownerPet)
                .allergenName("유제품")
                .symptom("설사")
                .severity(AllergySeverity.MILD)
                .identifiedAt(LocalDate.of(2025, 12, 1))
                .isActive(true)
                .memo("삭제 권한 테스트")
                .build());

        mockMvc.perform(delete("/api/pets/{petId}/allergies/{allergyId}", ownerPet.getId(), allergyRecord.getId())
                        .header("Authorization", "Bearer " + strangerAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫에 대한 권한이 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + ownerPet.getId() + "/allergies/" + allergyRecord.getId()));

        assertThat(allergyRecordRepository.findById(allergyRecord.getId())).isPresent();
        assertThat(allergyRecordRepository.count()).isEqualTo(1);
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
