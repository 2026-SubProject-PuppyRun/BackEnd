package org.zerock.puppyrun.auth.oauth2.DTO;

import com.mysema.commons.lang.Assert;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;

/**
 * 소셜 제공자 응답을 공통 규격으로 변환한 내부 사용자 프로필입니다.
 *
 * @param provider       사용자 정보를 제공한 소셜 로그인 제공자
 * @param providerUserId 소셜 제공자 내 사용자 식별자
 * @param email          소셜 제공자가 전달한 이메일
 */
public record OAuth2UserProfile(
        SocialProvider provider,
        String providerUserId,
        String email
) {
    public OAuth2UserProfile {
        Assert.notNull(provider, "소셜 로그인 제공자가 비어있습니다.");
        Assert.hasText(providerUserId, "소셜 사용자 식별자가 비어있습니다.");
        Assert.hasText(email, "소셜 이메일 제공 동의가 필요합니다.");
    }

}
