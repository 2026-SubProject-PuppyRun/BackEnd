package org.zerock.puppyrun.auth.oauth2.client;

import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;

/**
 * 소셜 제공자별 사용자 정보 조회 규격입니다.
 */
public interface OAuth2ProviderStrategy {

    /**
     * 전략이 지원하는 소셜 로그인 제공자를 반환합니다.
     *
     * @return 지원하는 소셜 로그인 제공자
     */
    SocialProvider getProvider();

    /**
     * 클라이언트가 전달한 액세스 토큰으로 소셜 제공자의 사용자 정보를 조회합니다.
     *
     * @param providerAccessToken 소셜 제공자가 발급한 액세스 토큰
     * @return 공통 형식으로 변환한 사용자 프로필
     */
    OAuth2UserProfile fetchUserProfile(String providerAccessToken);
}
