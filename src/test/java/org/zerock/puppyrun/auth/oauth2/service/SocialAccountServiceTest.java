package org.zerock.puppyrun.auth.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.auth.oauth2.entity.SocialAccount;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.auth.oauth2.repository.SocialAccountRepository;
import org.zerock.puppyrun.fixture.oauth2.SocialAccountFixture;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.ExistingUserException;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.support.SocialAccountTestData;

/**
 * 소셜 프로필을 회원과 연결하는 전체 저장 흐름을 검증합니다.
 */
class SocialAccountServiceTest extends TestContainerConfig {

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("이미 연결된 소셜 계정이면 기존 회원을 반환한다")
    void returnExistingSocialMember() {
        // given
        Member existingMember = new SocialAccountTestData(socialAccountService)
                .create(SocialAccountFixture.EXISTING_GOOGLE);

        // when
        Member result = socialAccountService.findOrCreateMember(
                SocialAccountFixture.EXISTING_GOOGLE.profile()
        );

        // then
        assertThat(result.getId()).isEqualTo(existingMember.getId());
        assertThat(memberRepository.count()).isEqualTo(1);
        assertThat(socialAccountRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("처음 로그인한 소셜 사용자는 회원과 소셜 계정을 하나의 기능으로 생성한다")
    void createMemberAndSocialAccount() {
        // given
        SocialAccountFixture fixture = SocialAccountFixture.NEW_GOOGLE;

        // when
        Member result = socialAccountService.findOrCreateMember(fixture.profile());

        // then
        SocialAccount savedAccount = socialAccountRepository
                .findByProviderAndProviderUserId(
                        SocialProvider.GOOGLE,
                        fixture.providerUserId()
                )
                .orElseThrow();
        assertThat(result.getEmail()).isEqualTo(fixture.email());
        assertThat(result.getNickName()).startsWith("google_");
        assertThat(result.getPassword()).isNotBlank();
        assertThat(savedAccount.getMember().getId()).isEqualTo(result.getId());
        assertThat(memberRepository.count()).isEqualTo(1);
        assertThat(socialAccountRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("동일 이메일의 기존 회원이 있으면 소셜 계정을 자동 연결하지 않는다")
    void rejectExistingEmailWithoutAccountLink() {
        // given
        new SocialAccountTestData(socialAccountService)
                .create(SocialAccountFixture.EXISTING_GOOGLE);

        // when
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
                () -> socialAccountService.findOrCreateMember(
                        SocialAccountFixture.EXISTING_EMAIL_WITH_NEW_GOOGLE_ID.profile()
                )
        );

        // then
        assertThat(thrown)
                .isInstanceOf(ExistingUserException.class)
                .hasMessageContaining("계정 연결이 필요합니다");
        assertThat(memberRepository.count()).isEqualTo(1);
        assertThat(socialAccountRepository.count()).isEqualTo(1);
    }
}
