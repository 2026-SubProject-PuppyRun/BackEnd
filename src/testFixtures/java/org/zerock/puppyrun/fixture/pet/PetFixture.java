package org.zerock.puppyrun.fixture.pet;

import java.time.LocalDate;
import org.zerock.puppyrun.pet.controller.request.RegisterPetRequest;

/**
 * 펫 등록 요청의 안정적인 기본값을 제공하는 Object Mother입니다.
 */
public enum PetFixture {

    MALTESE(
            "몽이",
            LocalDate.of(2022, 1, 1),
            "101",
            false,
            "M",
            "#FFFFFF"
    );

    private final String name;
    private final LocalDate birthDate;
    private final String breedId;
    private final boolean neutered;
    private final String gender;
    private final String profileColor;

    PetFixture(
            String name,
            LocalDate birthDate,
            String breedId,
            boolean neutered,
            String gender,
            String profileColor
    ) {
        this.name = name;
        this.birthDate = birthDate;
        this.breedId = breedId;
        this.neutered = neutered;
        this.gender = gender;
        this.profileColor = profileColor;
    }

    /**
     * 테스트마다 독립적인 펫 등록 요청을 생성합니다.
     *
     * @param weight 테스트 시나리오에서 사용할 초기 체중
     * @return 새 펫 등록 요청
     */
    public RegisterPetRequest request(double weight) {
        return new RegisterPetRequest(
                name,
                birthDate,
                breedId,
                neutered,
                gender,
                profileColor,
                weight
        );
    }
}
