package org.zerock.puppyrun.fixture.oauth2;

import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;

/**
 * 소셜 계정 시나리오별 프로필을 제공하는 Object Mother입니다.
 */
public enum SocialAccountFixture {

    EXISTING_GOOGLE("google-existing-id", "existing@example.com"),
    NEW_GOOGLE("google-new-id", "new@example.com"),
    EXISTING_EMAIL_WITH_NEW_GOOGLE_ID(
            "google-new-provider-id",
            "existing@example.com"
    );

    private final String providerUserId;
    private final String email;

    SocialAccountFixture(String providerUserId, String email) {
        this.providerUserId = providerUserId;
        this.email = email;
    }

    /**
     * 테스트마다 독립적인 소셜 사용자 프로필을 생성합니다.
     *
     * @return 새 소셜 사용자 프로필
     */
    public OAuth2UserProfile profile() {
        return new OAuth2UserProfile(
                SocialProvider.GOOGLE,
                providerUserId,
                email
        );
    }

    public String providerUserId() {
        return providerUserId;
    }

    public String email() {
        return email;
    }
}
