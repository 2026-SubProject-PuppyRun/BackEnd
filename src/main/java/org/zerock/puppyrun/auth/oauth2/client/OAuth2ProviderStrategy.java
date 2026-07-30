package org.zerock.puppyrun.auth.oauth2.client;

import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2Client;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;

/**
 * 소셜 제공자별 토큰 교환과 사용자 정보 조회 규격입니다.
 */
public interface OAuth2ProviderStrategy {

    /**
     * 전략이 지원하는 소셜 로그인 제공자를 반환합니다.
     *
     * @return 지원하는 소셜 로그인 제공자
     */
    SocialProvider getProvider();

    /**
     * 인가 코드를 소셜 제공자의 액세스 토큰으로 교환합니다.
     *
     * @param client 토큰 교환에 필요한 OAuth2 요청 정보
     * @return 소셜 제공자가 발급한 액세스 토큰
     */
    String exchangeAccessToken(OAuth2Client client);

    /**
     * 액세스 토큰으로 소셜 제공자의 사용자 정보를 조회합니다.
     *
     * @param accessToken 소셜 제공자가 발급한 액세스 토큰
     * @return 공통 형식으로 변환한 사용자 프로필
     */
    OAuth2UserProfile fetchUserProfile(String accessToken);
}
