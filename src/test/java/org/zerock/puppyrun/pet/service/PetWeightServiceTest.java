package org.zerock.puppyrun.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.fixture.member.MemberFixture;
import org.zerock.puppyrun.fixture.pet.PetFixture;
import org.zerock.puppyrun.member.service.MemberRegistrationService;
import org.zerock.puppyrun.pet.controller.request.RegisterPetWeightLogRequest;
import org.zerock.puppyrun.pet.controller.response.PetWeightRecordResponse;
import org.zerock.puppyrun.pet.entity.PetWeightLog;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.pet.repository.PetWeightLogRepository;
import org.zerock.puppyrun.support.MemberPetTestData;
import org.zerock.puppyrun.support.MemberPetTestData.OwnerPetData;
import org.zerock.puppyrun.support.MemberPetTestData.OwnerPetWithStrangerData;

/**
 * 펫 체중 변경과 체중 이력 관리 기능을 서비스 경계에서 검증합니다.
 */
class PetWeightServiceTest extends TestContainerConfig {

    @Autowired
    private PetCommandService petCommandService;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PetWeightLogRepository petWeightLogRepository;

    @Autowired
    private MemberRegistrationService memberRegistrationService;

    @Test
    @DisplayName("체중을 등록하면 펫의 현재 체중과 이력이 함께 변경된다")
    void registerWeightAndUpdatePet() {
        // given
        OwnerPetData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).create(
                MemberFixture.PET_OWNER,
                PetFixture.MALTESE,
                3.4
        );
        double newWeight = 4.2;

        // when
        PetWeightRecordResponse result = petCommandService.registerPetWeightLog(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterPetWeightLogRequest(newWeight)
        );

        // then
        var updatedPet = petRepository.findById(fixture.petId()).orElseThrow();
        PetWeightLog savedLog = petWeightLogRepository.findById(result.weightLogId()).orElseThrow();
        assertThat(updatedPet.getWeight()).isEqualTo(newWeight);
        assertThat(savedLog.getWeight()).isEqualTo(newWeight);
        assertThat(savedLog.getPet().getId()).isEqualTo(fixture.petId());
    }

    @Test
    @DisplayName("같은 날의 체중 변경은 최신 이력을 갱신하고 같은 값은 중복 저장하지 않는다")
    void reuseLatestWeightLogOnSameDay() {
        // given
        OwnerPetData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).create(
                MemberFixture.PET_OWNER,
                PetFixture.MALTESE,
                3.4
        );
        double firstWeight = 4.0;
        double changedWeight = 4.5;

        // when
        PetWeightRecordResponse first = petCommandService.registerPetWeightLog(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterPetWeightLogRequest(firstWeight)
        );
        PetWeightRecordResponse second = petCommandService.registerPetWeightLog(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterPetWeightLogRequest(changedWeight)
        );
        PetWeightRecordResponse duplicated = petCommandService.registerPetWeightLog(
                fixture.ownerPrincipal(),
                fixture.petId(),
                new RegisterPetWeightLogRequest(changedWeight)
        );

        // then
        assertThat(second.weightLogId()).isEqualTo(first.weightLogId());
        assertThat(duplicated.weightLogId()).isEqualTo(first.weightLogId());
        assertThat(petWeightLogRepository.count()).isEqualTo(1);
        assertThat(petWeightLogRepository.findById(first.weightLogId()).orElseThrow().getWeight())
                .isEqualTo(changedWeight);
    }

    @Test
    @DisplayName("다른 회원의 펫 체중은 변경할 수 없다")
    void rejectWeightChangeForNonOwner() {
        // given
        OwnerPetWithStrangerData fixture = new MemberPetTestData(
                memberRegistrationService,
                petCommandService
        ).createWithStranger(
                MemberFixture.PET_OWNER,
                MemberFixture.PET_STRANGER,
                PetFixture.MALTESE,
                3.4
        );
        double newWeight = 4.2;

        // when
        Throwable thrown = catchThrowable(() -> petCommandService.registerPetWeightLog(
                fixture.strangerPrincipal(),
                fixture.petId(),
                new RegisterPetWeightLogRequest(newWeight)
        ));

        // then
        assertThat(thrown).isInstanceOf(UserForbiddenException.class);
        assertThat(petRepository.findById(fixture.petId()).orElseThrow().getWeight())
                .isEqualTo(3.4);
        assertThat(petWeightLogRepository.count()).isEqualTo(1);
    }
}
