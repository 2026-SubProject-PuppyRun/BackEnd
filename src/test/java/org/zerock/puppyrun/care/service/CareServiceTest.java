package org.zerock.puppyrun.care.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.care.controller.request.RegisterAllergyRequest;
import org.zerock.puppyrun.care.controller.request.RegisterMedicationRequest;
import org.zerock.puppyrun.care.controller.request.RegisterVaccinationRequest;
import org.zerock.puppyrun.care.controller.request.UpdateAllergyRequest;
import org.zerock.puppyrun.care.controller.request.UpdateMedicationRequest;
import org.zerock.puppyrun.care.controller.request.UpdateVaccinationRequest;
import org.zerock.puppyrun.care.controller.response.AllergyListResponse;
import org.zerock.puppyrun.care.controller.response.AllergyRecordResponse;
import org.zerock.puppyrun.care.controller.response.CareCalendarResponse;
import org.zerock.puppyrun.care.controller.response.MedicationListResponse;
import org.zerock.puppyrun.care.controller.response.MedicationRecordResponse;
import org.zerock.puppyrun.care.controller.response.VaccinationListResponse;
import org.zerock.puppyrun.care.controller.response.VaccinationRecordResponse;
import org.zerock.puppyrun.care.entity.CareEventType;
import org.zerock.puppyrun.care.entity.AllergySeverity;
import org.zerock.puppyrun.care.repository.AllergyRecordRepository;
import org.zerock.puppyrun.care.repository.MedicationRecordRepository;
import org.zerock.puppyrun.care.repository.VaccinationRecordRepository;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.fixture.member.MemberFixture;
import org.zerock.puppyrun.fixture.pet.PetFixture;
import org.zerock.puppyrun.member.service.MemberRegistrationService;
import org.zerock.puppyrun.pet.controller.request.RegisterPetWeightLogRequest;
import org.zerock.puppyrun.pet.service.PetCommandService;
import org.zerock.puppyrun.support.MemberPetTestData;
import org.zerock.puppyrun.support.MemberPetTestData.OwnerPetData;
import org.zerock.puppyrun.support.MemberPetTestData.OwnerPetWithStrangerData;

/**
 * 케어 도메인의 등록·조회·수정·삭제 흐름을 서비스 경계에서 검증합니다.
 */
class CareServiceTest extends TestContainerConfig {

    @Autowired
    private AllergyCommandService allergyCommandService;

    @Autowired
    private AllergyQueryService allergyQueryService;

    @Autowired
    private MedicationCommandService medicationCommandService;

    @Autowired
    private MedicationQueryService medicationQueryService;

    @Autowired
    private VaccinationCommandService vaccinationCommandService;

    @Autowired
    private VaccinationQueryService vaccinationQueryService;

    @Autowired
    private CareCalendarQueryService careCalendarQueryService;

    @Autowired
    private PetCommandService petCommandService;

    @Autowired
    private MemberRegistrationService memberRegistrationService;

    @Autowired
    private AllergyRecordRepository allergyRecordRepository;

    @Autowired
    private MedicationRecordRepository medicationRecordRepository;

    @Autowired
    private VaccinationRecordRepository vaccinationRecordRepository;

    @Test
    @DisplayName("알러지 기록의 등록부터 삭제까지 하나의 기능 흐름으로 처리한다")
    void manageAllergyLifecycle() {
        // given
        OwnerPetData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).create(
                MemberFixture.CARE_OWNER,
                PetFixture.MALTESE,
                3.5
        );
        LocalDate diagnosedAt = LocalDate.of(2026, 4, 20);

        // when
        AllergyRecordResponse registered = allergyCommandService.registerAllergy(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterAllergyRequest(
                        "닭고기",
                        "피부 가려움",
                        "MODERATE",
                        diagnosedAt,
                        true,
                        "간식 섭취 후 반응"
                )
        );

        AllergyListResponse found = allergyQueryService.getAllergyList(
                fixture.ownerPrincipal(),
                fixture.petId()
        );
        AllergyRecordResponse updated = allergyCommandService.updateAllergy(
                fixture.ownerPrincipal(),
                fixture.petId(),
                registered.allergyId(),
                new UpdateAllergyRequest(
                        "소고기",
                        "눈물 증가",
                        "MILD",
                        LocalDate.of(2026, 4, 21),
                        false,
                        "사료 변경 후 호전"
                )
        );
        allergyCommandService.deleteAllergy(
                fixture.ownerPrincipal(),
                fixture.petId(),
                registered.allergyId()
        );

        // then
        assertThat(found.totalAllergyCount()).isEqualTo(1);
        assertThat(found.allergyList().getFirst().allergenName()).isEqualTo("닭고기");
        assertThat(updated.allergenName()).isEqualTo("소고기");
        assertThat(updated.severity()).isEqualTo("MILD");
        assertThat(updated.isActive()).isFalse();
        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("알러지 심각도 미입력 시 NONE으로 저장하고 조회한다")
    void registerAllergyWithoutSeverityStoresNone() {
        // given
        OwnerPetData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).create(
                MemberFixture.CARE_OWNER,
                PetFixture.MALTESE,
                3.5
        );

        // when
        AllergyRecordResponse registered = allergyCommandService.registerAllergy(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterAllergyRequest("닭고기", null, null, null, true, null)
        );
        AllergyListResponse found = allergyQueryService.getAllergyList(
                fixture.ownerPrincipal(),
                fixture.petId()
        );

        // then
        assertThat(registered.severity()).isEqualTo("NONE");
        assertThat(found.allergyList().getFirst().severity()).isEqualTo("NONE");
        assertThat(allergyRecordRepository.findById(registered.allergyId()).orElseThrow().getSeverity())
                .isEqualTo(AllergySeverity.NONE);
    }

    @Test
    @DisplayName("알러지 심각도는 수정 시 미입력하면 NONE으로 변경한다")
    void updateAllergyWithoutSeverityStoresNone() {
        // given
        OwnerPetData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).create(
                MemberFixture.CARE_OWNER,
                PetFixture.MALTESE,
                3.5
        );
        AllergyRecordResponse registered = allergyCommandService.registerAllergy(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterAllergyRequest("닭고기", null, "SEVERE", null, true, null)
        );

        // when
        AllergyRecordResponse updated = allergyCommandService.updateAllergy(
                fixture.ownerPrincipal(),
                fixture.petId(),
                registered.allergyId(),
                new UpdateAllergyRequest("닭고기", null, " ", null, true, null)
        );

        // then
        assertThat(updated.severity()).isEqualTo("NONE");
        assertThat(allergyRecordRepository.findById(registered.allergyId()).orElseThrow().getSeverity())
                .isEqualTo(AllergySeverity.NONE);
    }

    @Test
    @DisplayName("알러지 심각도는 정의되지 않은 값이면 예외를 발생시킨다")
    void registerAllergyWithInvalidSeverityThrowsException() {
        // given
        OwnerPetData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).create(
                MemberFixture.CARE_OWNER,
                PetFixture.MALTESE,
                3.5
        );

        // when
        Throwable thrown = catchThrowable(() -> allergyCommandService.registerAllergy(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterAllergyRequest("닭고기", null, "UNKNOWN", null, true, null)
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidValueException.class);
        assertThat(allergyRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("투약 기록의 등록·정렬 조회·수정·삭제 흐름을 처리한다")
    void manageMedicationLifecycle() {
        // given
        OwnerPetData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).create(
                MemberFixture.CARE_OWNER,
                PetFixture.MALTESE,
                3.5
        );
        LocalDateTime firstTime = LocalDateTime.of(2026, 4, 20, 9, 0);

        // when
        MedicationRecordResponse first = medicationCommandService.registerMedication(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterMedicationRequest("예방약", firstTime, 1.0, "tablet", "아침 식후")
        );
        medicationCommandService.registerMedication(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterMedicationRequest("영양제", firstTime.plusDays(1), 2.0, "ml", null)
        );

        MedicationListResponse found = medicationQueryService.getMedicationList(
                fixture.ownerPrincipal(),
                fixture.petId()
        );
        MedicationRecordResponse updated = medicationCommandService.updateMedication(
                fixture.ownerPrincipal(),
                fixture.petId(),
                first.medicationLogId(),
                new UpdateMedicationRequest("심장사상충 예방약", firstTime.plusHours(1), 1.5, "tablet", "저녁 식후")
        );
        medicationCommandService.deleteMedication(
                fixture.ownerPrincipal(),
                fixture.petId(),
                first.medicationLogId()
        );

        // then
        assertThat(found.totalMedicationCount()).isEqualTo(2);
        assertThat(found.medicationLogList())
                .extracting(MedicationListResponse.MedicationLog::medicationName)
                .containsExactly("영양제", "예방약");
        assertThat(updated.medicationName()).isEqualTo("심장사상충 예방약");
        assertThat(updated.doseAmount()).isEqualTo(1.5);
        assertThat(medicationRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("예방접종 기록 생명주기와 접종일 규칙을 함께 검증한다")
    void manageVaccinationLifecycle() {
        // given
        OwnerPetData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).create(
                MemberFixture.CARE_OWNER,
                PetFixture.MALTESE,
                3.5
        );
        LocalDate vaccinatedAt = LocalDate.of(2026, 4, 20);

        // when
        VaccinationRecordResponse registered = vaccinationCommandService.registerVaccination(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterVaccinationRequest(
                        "종합백신 4차",
                        vaccinatedAt,
                        vaccinatedAt.plusYears(1),
                        "퍼피동물병원",
                        null
                )
        );

        VaccinationListResponse found = vaccinationQueryService.getVaccinationList(
                fixture.ownerPrincipal(),
                fixture.petId()
        );
        VaccinationRecordResponse updated = vaccinationCommandService.updateVaccination(
                fixture.ownerPrincipal(),
                fixture.petId(),
                registered.vaccinationId(),
                new UpdateVaccinationRequest(
                        "종합백신 5차",
                        vaccinatedAt.plusMonths(1),
                        vaccinatedAt.plusYears(1).plusMonths(1),
                        "퍼피동물병원",
                        "특이 반응 없음"
                )
        );
        Throwable invalidDateThrown = catchThrowable(() -> vaccinationCommandService.registerVaccination(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterVaccinationRequest(
                        "광견병",
                        vaccinatedAt,
                        vaccinatedAt.minusDays(1),
                        null,
                        null
                )
        ));
        vaccinationCommandService.deleteVaccination(
                fixture.ownerPrincipal(),
                fixture.petId(),
                registered.vaccinationId()
        );

        // then
        assertThat(found.totalVaccinationCount()).isEqualTo(1);
        assertThat(found.vaccinationList().getFirst().vaccineName()).isEqualTo("종합백신 4차");
        assertThat(updated.vaccineName()).isEqualTo("종합백신 5차");
        assertThat(invalidDateThrown).isInstanceOf(InvalidValueException.class);
        assertThat(vaccinationRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("체중·접종·투약·알러지 기록을 케어 캘린더 기능으로 통합 조회한다")
    void getIntegratedCareCalendar() {
        // given
        OwnerPetData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).create(
                MemberFixture.CARE_OWNER,
                PetFixture.MALTESE,
                3.5
        );
        LocalDate today = LocalDate.now();
        petCommandService.registerPetWeightLog(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterPetWeightLogRequest(4.2)
        );
        vaccinationCommandService.registerVaccination(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterVaccinationRequest("종합백신", today.minusDays(1), today.plusYears(1), "퍼피동물병원", null)
        );
        medicationCommandService.registerMedication(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterMedicationRequest("예방약", today.plusDays(1).atTime(9, 0), 1.0, "tablet", null)
        );
        allergyCommandService.registerAllergy(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterAllergyRequest("닭고기", "피부 가려움", "MODERATE", today, true, null)
        );

        // when
        CareCalendarResponse result = careCalendarQueryService.getCareCalendar(
                fixture.ownerPrincipal(),
                fixture.petId(),
                today.minusDays(1),
                today.plusDays(1)
        );

        // then
        assertThat(result.totalEventCount()).isEqualTo(4);
        assertThat(result.eventList())
                .extracting(CareCalendarResponse.CareEvent::eventType)
                .containsExactly(
                        CareEventType.VACCINATION,
                        CareEventType.ALLERGY,
                        CareEventType.WEIGHT,
                        CareEventType.MEDICATION
                );
    }

    @Test
    @DisplayName("다른 회원의 펫에는 케어 서비스를 사용할 수 없다")
    void rejectCareAccessForNonOwner() {
        // given
        OwnerPetWithStrangerData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).createWithStranger(
                MemberFixture.CARE_OWNER,
                MemberFixture.CARE_STRANGER,
                PetFixture.MALTESE,
                3.5
        );
        LocalDate targetDate = LocalDate.of(2026, 4, 20);

        // when
        Throwable allergyThrown = catchThrowable(
                () -> allergyQueryService.getAllergyList(
                        fixture.strangerPrincipal(),
                        fixture.petId()
                )
        );
        Throwable calendarThrown = catchThrowable(() -> careCalendarQueryService.getCareCalendar(
                fixture.strangerPrincipal(),
                fixture.petId(),
                targetDate,
                targetDate
        ));

        // then
        assertThat(allergyThrown).isInstanceOf(UserForbiddenException.class);
        assertThat(calendarThrown).isInstanceOf(UserForbiddenException.class);
    }
}
