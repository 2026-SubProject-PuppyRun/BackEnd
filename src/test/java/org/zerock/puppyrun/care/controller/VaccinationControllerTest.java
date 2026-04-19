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
import org.zerock.puppyrun.care.controller.request.RegisterVaccinationRequest;
import org.zerock.puppyrun.care.controller.request.UpdateVaccinationRequest;
import org.zerock.puppyrun.care.entity.VaccinationRecord;
import org.zerock.puppyrun.care.repository.VaccinationRecordRepository;
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
class VaccinationControllerTest {

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
    private VaccinationRecordRepository vaccinationRecordRepository;

    @BeforeEach
    void setUp() {
        vaccinationRecordRepository.deleteAll();
        petRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("접종 기록 등록 API")
    void registerVaccination() throws Exception {
        Member member = createMember("register@test.com", "registerUser");
        Pet pet = createPet(member, "몽이");
        String accessToken = createAccessToken(member);

        RegisterVaccinationRequest request = new RegisterVaccinationRequest(
                "종합백신 5차",
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2027, 4, 20),
                "퍼피동물병원",
                "특이 반응 없음"
        );

        mockMvc.perform(post("/api/pets/{petId}/vaccinations", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.vaccine_name").value("종합백신 5차"))
                .andExpect(jsonPath("$.vaccinated_at").value("2026-04-20"))
                .andExpect(jsonPath("$.next_vaccination_date").value("2027-04-20"))
                .andExpect(jsonPath("$.hospital_name").value("퍼피동물병원"))
                .andExpect(jsonPath("$.memo").value("특이 반응 없음"));

        assertThat(vaccinationRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("접종 기록 목록 조회 API")
    void getVaccinationList() throws Exception {
        Member member = createMember("list@test.com", "listUser");
        Pet pet = createPet(member, "두부");
        String accessToken = createAccessToken(member);

        vaccinationRecordRepository.save(VaccinationRecord.builder()
                .pet(pet)
                .vaccineName("광견병")
                .vaccinatedAt(LocalDate.of(2025, 10, 1))
                .nextVaccinationDate(LocalDate.of(2026, 10, 1))
                .hospitalName("퍼피동물병원")
                .memo(null)
                .build());

        vaccinationRecordRepository.save(VaccinationRecord.builder()
                .pet(pet)
                .vaccineName("종합백신 5차")
                .vaccinatedAt(LocalDate.of(2026, 4, 20))
                .nextVaccinationDate(LocalDate.of(2027, 4, 20))
                .hospitalName("퍼피동물병원")
                .memo("특이 반응 없음")
                .build());

        mockMvc.perform(get("/api/pets/{petId}/vaccinations", pet.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.total_vaccination_count").value(2))
                .andExpect(jsonPath("$.vaccination_list[0].vaccine_name").value("종합백신 5차"))
                .andExpect(jsonPath("$.vaccination_list[0].vaccinated_at").value("2026-04-20"))
                .andExpect(jsonPath("$.vaccination_list[1].vaccine_name").value("광견병"))
                .andExpect(jsonPath("$.vaccination_list[1].vaccinated_at").value("2025-10-01"));
    }

    @Test
    @DisplayName("접종 기록 수정 API")
    void updateVaccination() throws Exception {
        Member member = createMember("update@test.com", "updateUser");
        Pet pet = createPet(member, "보리");
        String accessToken = createAccessToken(member);

        VaccinationRecord vaccinationRecord = vaccinationRecordRepository.save(VaccinationRecord.builder()
                .pet(pet)
                .vaccineName("종합백신 4차")
                .vaccinatedAt(LocalDate.of(2025, 4, 20))
                .nextVaccinationDate(LocalDate.of(2026, 4, 20))
                .hospitalName("기존동물병원")
                .memo("초기 메모")
                .build());

        UpdateVaccinationRequest request = new UpdateVaccinationRequest(
                "종합백신 5차",
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2027, 5, 1),
                "퍼피동물병원",
                "추가 경과 관찰 예정"
        );

        mockMvc.perform(put("/api/pets/{petId}/vaccinations/{vaccinationId}", pet.getId(), vaccinationRecord.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vaccination_id").value(vaccinationRecord.getId().toString()))
                .andExpect(jsonPath("$.vaccine_name").value("종합백신 5차"))
                .andExpect(jsonPath("$.vaccinated_at").value("2026-04-20"))
                .andExpect(jsonPath("$.next_vaccination_date").value("2027-05-01"))
                .andExpect(jsonPath("$.hospital_name").value("퍼피동물병원"))
                .andExpect(jsonPath("$.memo").value("추가 경과 관찰 예정"));

        VaccinationRecord updated = vaccinationRecordRepository.findById(vaccinationRecord.getId()).orElseThrow();
        assertThat(updated.getVaccineName()).isEqualTo("종합백신 5차");
        assertThat(updated.getHospitalName()).isEqualTo("퍼피동물병원");
        assertThat(updated.getNextVaccinationDate()).isEqualTo(LocalDate.of(2027, 5, 1));
    }

    @Test
    @DisplayName("접종 기록 삭제 API")
    void deleteVaccination() throws Exception {
        Member member = createMember("delete@test.com", "deleteUser");
        Pet pet = createPet(member, "해피");
        String accessToken = createAccessToken(member);

        VaccinationRecord vaccinationRecord = vaccinationRecordRepository.save(VaccinationRecord.builder()
                .pet(pet)
                .vaccineName("광견병")
                .vaccinatedAt(LocalDate.of(2026, 1, 10))
                .nextVaccinationDate(LocalDate.of(2027, 1, 10))
                .hospitalName("퍼피동물병원")
                .memo("삭제 대상")
                .build());

        mockMvc.perform(delete("/api/pets/{petId}/vaccinations/{vaccinationId}", pet.getId(), vaccinationRecord.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        assertThat(vaccinationRecordRepository.findById(vaccinationRecord.getId())).isEmpty();
        assertThat(vaccinationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 펫으로 접종 기록 등록 요청 시 404를 반환한다")
    void registerVaccination_whenPetNotFound() throws Exception {
        Member member = createMember("pet-not-found@test.com", "petNotFoundUser");
        String accessToken = createAccessToken(member);
        UUID notFoundPetId = UUID.randomUUID();

        RegisterVaccinationRequest request = new RegisterVaccinationRequest(
                "종합백신 5차",
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2027, 4, 20),
                "퍼피동물병원",
                "특이 반응 없음"
        );

        mockMvc.perform(post("/api/pets/{petId}/vaccinations", notFoundPetId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + notFoundPetId + "/vaccinations"));

        assertThat(vaccinationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 접종 기록 수정 요청 시 404를 반환한다")
    void updateVaccination_whenVaccinationNotFound() throws Exception {
        Member member = createMember("vaccination-not-found@test.com", "vaccinationNotFoundUser");
        Pet pet = createPet(member, "콩이");
        String accessToken = createAccessToken(member);
        UUID notFoundVaccinationId = UUID.randomUUID();

        UpdateVaccinationRequest request = new UpdateVaccinationRequest(
                "종합백신 5차",
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2027, 4, 20),
                "퍼피동물병원",
                "수정 시도"
        );

        mockMvc.perform(put("/api/pets/{petId}/vaccinations/{vaccinationId}", pet.getId(), notFoundVaccinationId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("해당 접종 기록을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/pets/" + pet.getId() + "/vaccinations/" + notFoundVaccinationId));

        assertThat(vaccinationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("다른 사용자의 펫 접종 기록 조회 요청 시 403을 반환한다")
    void getVaccinationList_whenPetOwnedByOtherUser() throws Exception {
        Member owner = createMember("owner@test.com", "ownerUser");
        Member stranger = createMember("stranger@test.com", "strangerUser");
        Pet ownerPet = createPet(owner, "주인강아지");
        String strangerAccessToken = createAccessToken(stranger);

        vaccinationRecordRepository.save(VaccinationRecord.builder()
                .pet(ownerPet)
                .vaccineName("광견병")
                .vaccinatedAt(LocalDate.of(2026, 4, 20))
                .nextVaccinationDate(LocalDate.of(2027, 4, 20))
                .hospitalName("퍼피동물병원")
                .memo("권한 테스트")
                .build());

        mockMvc.perform(get("/api/pets/{petId}/vaccinations", ownerPet.getId())
                        .header("Authorization", "Bearer " + strangerAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫에 대한 권한이 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + ownerPet.getId() + "/vaccinations"));

        assertThat(vaccinationRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("다음 접종 예정일이 접종일보다 이전이면 등록에 실패한다")
    void registerVaccination_whenNextVaccinationDateBeforeVaccinatedAt() throws Exception {
        Member member = createMember("invalid-date@test.com", "invalidDateUser");
        Pet pet = createPet(member, "토리");
        String accessToken = createAccessToken(member);

        RegisterVaccinationRequest request = new RegisterVaccinationRequest(
                "종합백신 5차",
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 19),
                "퍼피동물병원",
                "잘못된 날짜"
        );

        mockMvc.perform(post("/api/pets/{petId}/vaccinations", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("다음 접종 예정일은 접종일보다 이전일 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/vaccinations"));

        assertThat(vaccinationRecordRepository.count()).isZero();
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
