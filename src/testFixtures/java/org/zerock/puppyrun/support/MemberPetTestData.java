package org.zerock.puppyrun.support;

import java.util.UUID;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.fixture.member.MemberFixture;
import org.zerock.puppyrun.fixture.pet.PetFixture;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.member.service.MemberRegistrationService;
import org.zerock.puppyrun.pet.controller.response.PetDetailResponse;
import org.zerock.puppyrun.pet.service.PetCommandService;

/**
 * 회원과 펫 픽스처를 실제 서비스 로직으로 조합하는 테스트 데이터 생성기입니다.
 */
public final class MemberPetTestData {

    private final MemberRegistrationService memberRegistrationService;
    private final PetCommandService petCommandService;

    public MemberPetTestData(
            MemberRegistrationService memberRegistrationService,
            PetCommandService petCommandService
    ) {
        this.memberRegistrationService = memberRegistrationService;
        this.petCommandService = petCommandService;
    }

    /**
     * 소유자를 가입시키고 소유자의 펫을 등록합니다.
     *
     * @param owner 소유자 픽스처
     * @param pet 펫 픽스처
     * @param initialWeight 펫의 초기 체중
     * @return 소유자 인증 정보와 펫 식별자
     */
    public OwnerPetData create(
            MemberFixture owner,
            PetFixture pet,
            double initialWeight
    ) {
        MemberDTO ownerMember = register(owner);
        UserPrincipal ownerPrincipal = principal(ownerMember);
        PetDetailResponse savedPet = petCommandService.registerPet(
                ownerPrincipal,
                pet.request(initialWeight)
        );

        return new OwnerPetData(
                ownerPrincipal,
                savedPet.PetId()
        );
    }

    /**
     * 소유자와 펫을 만든 뒤 접근 권한이 없는 다른 회원을 추가합니다.
     *
     * @param owner 소유자 픽스처
     * @param stranger 다른 회원 픽스처
     * @param pet 펫 픽스처
     * @param initialWeight 펫의 초기 체중
     * @return 소유자·다른 회원 인증 정보와 펫 식별자
     */
    public OwnerPetWithStrangerData createWithStranger(
            MemberFixture owner,
            MemberFixture stranger,
            PetFixture pet,
            double initialWeight
    ) {
        OwnerPetData ownerPet = create(owner, pet, initialWeight);

        return new OwnerPetWithStrangerData(
                ownerPet.ownerPrincipal(),
                principal(register(stranger)),
                ownerPet.petId()
        );
    }

    private MemberDTO register(MemberFixture fixture) {
        return memberRegistrationService.registerLocalMember(
                fixture.email(),
                fixture.nickname(),
                fixture.password()
        );
    }

    private UserPrincipal principal(MemberDTO member) {
        return new UserPrincipal(member.id(), member.email(), member.userRole());
    }

    /**
     * 회원·펫 생성 결과를 테스트에 전달하는 값 객체입니다.
     */
    public record OwnerPetData(
            UserPrincipal ownerPrincipal,
            UUID petId
    ) {
    }

    /**
     * 소유권 검증용 회원·펫 생성 결과를 전달하는 값 객체입니다.
     */
    public record OwnerPetWithStrangerData(
            UserPrincipal ownerPrincipal,
            UserPrincipal strangerPrincipal,
            UUID petId
    ) {
    }
}
