package org.zerock.puppyrun.care.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.zerock.puppyrun.care.entity.AllergyRecord;
import org.zerock.puppyrun.care.entity.AllergySeverity;
import org.zerock.puppyrun.care.entity.MedicationRecord;
import org.zerock.puppyrun.care.entity.VaccinationRecord;
import org.zerock.puppyrun.care.repository.AllergyRecordRepository;
import org.zerock.puppyrun.care.repository.MedicationRecordRepository;
import org.zerock.puppyrun.care.repository.VaccinationRecordRepository;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.common.exception.ErrorCode;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.pet.entity.Breed;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetWeightLog;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.pet.repository.PetWeightLogRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CareCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PetWeightLogRepository petWeightLogRepository;

    @Autowired
    private VaccinationRecordRepository vaccinationRecordRepository;

    @Autowired
    private MedicationRecordRepository medicationRecordRepository;

    @Autowired
    private AllergyRecordRepository allergyRecordRepository;

    @BeforeEach
    void setUp() {
        allergyRecordRepository.deleteAll();
        medicationRecordRepository.deleteAll();
        vaccinationRecordRepository.deleteAll();
        petWeightLogRepository.deleteAll();
        petRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("케어 캘린더 통합 조회 API")
    void getCareCalendar() throws Exception {
        Member member = createMember("calendar@test.com", "calendarUser");
        Pet pet = createPet(member, "몽이");
        String accessToken = createAccessToken(member);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(1);
        LocalDate endDate = today.plusDays(1);

        PetWeightLog weightLog = petWeightLogRepository.save(PetWeightLog.builder()
                .pet(pet)
                .weight(4.2)
                .build());

        VaccinationRecord vaccinationRecord = vaccinationRecordRepository.save(VaccinationRecord.builder()
                .pet(pet)
                .vaccineName("종합백신")
                .vaccinatedAt(today.minusDays(1))
                .nextVaccinationDate(today.plusYears(1).minusDays(1))
                .hospitalName("퍼피동물병원")
                .memo("특이 반응 없음")
                .build());

        AllergyRecord allergyRecord = allergyRecordRepository.save(AllergyRecord.builder()
                .pet(pet)
                .allergenName("닭고기")
                .symptom("피부 가려움")
                .severity(AllergySeverity.MODERATE)
                .identifiedAt(today)
                .isActive(true)
                .memo("간식 섭취 후 반응")
                .build());

        medicationRecordRepository.save(MedicationRecord.builder()
                .pet(pet)
                .medicationName("심장사상충 예방약")
                .administeredAt(today.plusDays(1).atTime(9, 0))
                .doseAmount(1.0)
                .doseUnit("tablet")
                .memo("식후 투약")
                .build());

        allergyRecordRepository.save(AllergyRecord.builder()
                .pet(pet)
                .allergenName("우유")
                .symptom("설사")
                .severity(AllergySeverity.MILD)
                .identifiedAt(null)
                .isActive(true)
                .memo("날짜 없음")
                .build());

        mockMvc.perform(get("/api/pets/{petId}/care-calendar", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet_id").value(pet.getId().toString()))
                .andExpect(jsonPath("$.start_date").value(startDate.toString()))
                .andExpect(jsonPath("$.end_date").value(endDate.toString()))
                .andExpect(jsonPath("$.total_event_count").value(4))
                .andExpect(jsonPath("$.event_list[0].date").value(today.minusDays(1).toString()))
                .andExpect(jsonPath("$.event_list[0].event_type").value("VACCINATION"))
                .andExpect(jsonPath("$.event_list[0].title").value("종합백신"))
                .andExpect(jsonPath("$.event_list[0].related_id").value(vaccinationRecord.getId().toString()))
                .andExpect(jsonPath("$.event_list[1].date").value(today.toString()))
                .andExpect(jsonPath("$.event_list[1].event_type").value("ALLERGY"))
                .andExpect(jsonPath("$.event_list[1].title").value("닭고기"))
                .andExpect(jsonPath("$.event_list[1].related_id").value(allergyRecord.getId().toString()))
                .andExpect(jsonPath("$.event_list[2].date").value(today.toString()))
                .andExpect(jsonPath("$.event_list[2].event_type").value("WEIGHT"))
                .andExpect(jsonPath("$.event_list[2].title").value("체중 기록"))
                .andExpect(jsonPath("$.event_list[2].related_id").value(weightLog.getId().toString()))
                .andExpect(jsonPath("$.event_list[3].date").value(today.plusDays(1).toString()))
                .andExpect(jsonPath("$.event_list[3].event_type").value("MEDICATION"))
                .andExpect(jsonPath("$.event_list[3].title").value("심장사상충 예방약"));

        assertThat(allergyRecordRepository.count()).isEqualTo(2);
        assertThat(vaccinationRecordRepository.count()).isEqualTo(1);
        assertThat(medicationRecordRepository.count()).isEqualTo(1);
        assertThat(petWeightLogRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 펫으로 케어 캘린더 조회 요청 시 404를 반환한다")
    void getCareCalendar_whenPetNotFound() throws Exception {
        Member member = createMember("calendar-not-found@test.com", "calendarNotFoundUser");
        String accessToken = createAccessToken(member);
        LocalDate today = LocalDate.now();
        UUID notFoundPetId = UUID.randomUUID();

        mockMvc.perform(get("/api/pets/{petId}/care-calendar", notFoundPetId)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("startDate", today.minusDays(1).toString())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + notFoundPetId + "/care-calendar"));
    }

    @Test
    @DisplayName("다른 사용자의 펫 케어 캘린더 조회 요청 시 403을 반환한다")
    void getCareCalendar_whenPetOwnedByOtherUser() throws Exception {
        Member owner = createMember("calendar-owner@test.com", "calendarOwnerUser");
        Member stranger = createMember("calendar-stranger@test.com", "calendarStrangerUser");
        Pet ownerPet = createPet(owner, "주인강아지");
        String strangerAccessToken = createAccessToken(stranger);
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/pets/{petId}/care-calendar", ownerPet.getId())
                        .header("Authorization", "Bearer " + strangerAccessToken)
                        .param("startDate", today.minusDays(1).toString())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("해당 펫에 대한 권한이 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + ownerPet.getId() + "/care-calendar"));
    }

    @Test
    @DisplayName("조회 시작일이 종료일보다 늦으면 케어 캘린더 조회에 실패한다")
    void getCareCalendar_whenDateRangeIsInvalid() throws Exception {
        Member member = createMember("calendar-invalid-date@test.com", "calendarInvalidDateUser");
        Pet pet = createPet(member, "보리");
        String accessToken = createAccessToken(member);
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/pets/{petId}/care-calendar", pet.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .param("startDate", today.plusDays(1).toString())
                        .param("endDate", today.minusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("조회 시작일은 종료일보다 늦을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/care-calendar"));
    }

    @Test
    @DisplayName("인증 없이 케어 캘린더 조회 요청 시 401을 반환한다")
    void getCareCalendar_whenUnauthenticated() throws Exception {
        Member member = createMember("calendar-unauthenticated@test.com", "calendarUnauthenticatedUser");
        Pet pet = createPet(member, "나비");
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/pets/{petId}/care-calendar", pet.getId())
                        .param("startDate", today.minusDays(1).toString())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.MISSING_AUTHORIZATION_HEADER.getCode()))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/care-calendar"));
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 케어 캘린더 조회 요청 시 401을 반환한다")
    void getCareCalendar_whenInvalidToken() throws Exception {
        Member member = createMember("calendar-invalid-token@test.com", "calendarInvalidTokenUser");
        Pet pet = createPet(member, "구름");
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/pets/{petId}/care-calendar", pet.getId())
                        .header("Authorization", "Bearer invalid-token")
                        .param("startDate", today.minusDays(1).toString())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN.getCode()))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.path").value("/api/pets/" + pet.getId() + "/care-calendar"));
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
