package org.zerock.puppyrun.fixture.member;

/**
 * 통합 테스트에서 사용하는 회원 기본값을 정의하는 Object Mother입니다.
 *
 * <p>저장은 담당하지 않으며, 테스트가 필요한 회원 유형과 입력값만 선택할 수 있게 합니다.</p>
 */
public enum MemberFixture {

    CARE_OWNER("care-owner@test.com", "care-owner"),
    CARE_STRANGER("care-stranger@test.com", "care-stranger"),
    PET_OWNER("pet-owner@test.com", "pet-owner"),
    PET_STRANGER("pet-stranger@test.com", "pet-stranger");

    private static final String DEFAULT_PASSWORD = "encoded-password";

    private final String email;
    private final String nickname;

    MemberFixture(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
    }

    public String email() {
        return email;
    }

    public String nickname() {
        return nickname;
    }

    public String password() {
        return DEFAULT_PASSWORD;
    }
}
