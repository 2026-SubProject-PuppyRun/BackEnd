package org.zerock.puppyrun.auth.oauth2.DTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;

class OAuth2UserProfileTest {

    @Test
    @DisplayName("유효한 소셜 사용자 정보로 프로필을 생성한다")
    void createProfile() {
        // given
        SocialProvider provider = SocialProvider.KAKAO;

        // when
        OAuth2UserProfile profile = new OAuth2UserProfile(
                provider,
                "123456789",
                "puppy@example.com"
        );

        // then
        assertThat(profile.provider()).isEqualTo(provider);
        assertThat(profile.providerUserId()).isEqualTo("123456789");
        assertThat(profile.email()).isEqualTo("puppy@example.com");
    }

    @Test
    @DisplayName("소셜 로그인 제공자가 없으면 프로필 생성을 거부한다")
    void rejectMissingProvider() {
        // given
        SocialProvider provider = null;

        // when
        Throwable thrown = catchThrowable(() -> new OAuth2UserProfile(
                provider,
                "123456789",
                "puppy@example.com"
        ));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 로그인 제공자가 비어있습니다.");
    }

    @Test
    @DisplayName("소셜 사용자 식별자가 공백이면 프로필 생성을 거부한다")
    void rejectBlankProviderUserId() {
        // given
        String providerUserId = " ";

        // when
        Throwable thrown = catchThrowable(() -> new OAuth2UserProfile(
                SocialProvider.KAKAO,
                providerUserId,
                "puppy@example.com"
        ));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 사용자 식별자가 비어있습니다.");
    }

    @Test
    @DisplayName("소셜 이메일이 공백이면 프로필 생성을 거부한다")
    void rejectBlankEmail() {
        // given
        String email = " ";

        // when
        Throwable thrown = catchThrowable(() -> new OAuth2UserProfile(
                SocialProvider.KAKAO,
                "123456789",
                email
        ));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 이메일 제공 동의가 필요합니다.");
    }

}
