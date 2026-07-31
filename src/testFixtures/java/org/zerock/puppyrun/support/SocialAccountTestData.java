package org.zerock.puppyrun.support;

import org.zerock.puppyrun.auth.oauth2.service.SocialAccountService;
import org.zerock.puppyrun.fixture.oauth2.SocialAccountFixture;
import org.zerock.puppyrun.member.entity.Member;

/**
 * 소셜 계정 픽스처를 실제 서비스 로직으로 저장하는 테스트 데이터 생성기입니다.
 */
public final class SocialAccountTestData {

    private final SocialAccountService socialAccountService;

    public SocialAccountTestData(SocialAccountService socialAccountService) {
        this.socialAccountService = socialAccountService;
    }

    /**
     * 선택한 소셜 프로필로 회원 조회 또는 가입 흐름을 실행합니다.
     *
     * @param fixture 소셜 계정 픽스처
     * @return 조회되거나 생성된 회원
     */
    public Member create(SocialAccountFixture fixture) {
        return socialAccountService.findOrCreateMember(fixture.profile());
    }
}
