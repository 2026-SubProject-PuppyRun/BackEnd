package org.zerock.puppyrun.auth.oauth2.DTO;

import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;

/**
 * 소셜 제공자의 토큰 API 호출에 필요한 내부 요청 정보입니다.
 *
 * @param provider          소셜 로그인 제공자
 * @param authorizationCode 클라이언트가 전달한 인가 코드
 * @param redirectUrl       인가 요청에 사용한 리다이렉트 URL
 */
public record OAuth2Client(
        SocialProvider provider,
        String authorizationCode,
        String redirectUrl
) {
}
