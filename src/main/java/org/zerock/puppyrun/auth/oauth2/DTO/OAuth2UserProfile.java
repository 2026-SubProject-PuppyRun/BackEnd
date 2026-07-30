package org.zerock.puppyrun.auth.oauth2.DTO;

import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;

/**
 * 소셜 제공자 응답을 공통 규격으로 변환한 내부 사용자 프로필입니다.
 *
 * @param provider       사용자 정보를 제공한 소셜 로그인 제공자
 * @param providerUserId 소셜 제공자 내 사용자 식별자
 * @param email          소셜 제공자가 전달한 이메일
 * @param nickName       소셜 제공자가 전달한 닉네임
 */
public record OAuth2UserProfile(
        SocialProvider provider,
        String providerUserId,
        String email,
        String nickName
) {
}
