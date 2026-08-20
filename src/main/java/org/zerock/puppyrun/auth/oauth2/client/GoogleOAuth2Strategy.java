package org.zerock.puppyrun.auth.oauth2.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.commons.lang.Assert;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;
import org.zerock.puppyrun.auth.oauth2.exception.OAuth2AuthenticationException;
import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ExternalApiParsingException;
import reactor.core.publisher.Mono;

/**
 * Google OAuth 로그인 전략입니다.
 */
@Component
@RequiredArgsConstructor
public class GoogleOAuth2Strategy implements OAuth2ProviderStrategy {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final String USER_INFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private final WebClient webClient;

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public OAuth2UserProfile fetchUserProfile(String providerAccessToken) {
        Assert.hasText(providerAccessToken, "Google 액세스 토큰이 비어있습니다.");
        return webClient.get()
                .uri(USER_INFO_URL)
                .headers(headers -> headers.setBearerAuth(providerAccessToken))
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.releaseBody()
                                .then(Mono.error(new OAuth2AuthenticationException(
                                        "Google 액세스 토큰이 유효하지 않습니다."
                                )))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.releaseBody()
                                .then(Mono.error(new ExternalApiParsingException(
                                        "Google 사용자 정보 API 호출에 실패했습니다."
                                )))
                )
                .bodyToMono(GoogleUserResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .map(this::toUserProfile)
                .onErrorMap(
                        error -> !(error instanceof BusinessException),
                        error -> new ExternalApiParsingException("Google 사용자 정보 API와 통신할 수 없습니다.", error)
                )
                .blockOptional()
                .orElseThrow(() -> new ExternalApiParsingException("Google 사용자 정보 응답이 비어있습니다."));
    }

    private OAuth2UserProfile toUserProfile(GoogleUserResponse user) {
        if (user.sub() == null || user.sub().isBlank()
                || user.email() == null || user.email().isBlank()
                || !Boolean.TRUE.equals(user.emailVerified())
        ) {
            throw new OAuth2AuthenticationException("Google 가입에 필요한 사용자 정보가 비어있습니다.");
        }

        return new OAuth2UserProfile(
                getProvider(),
                user.sub(),
                user.email()
        );
    }

    private record GoogleUserResponse(
            String sub,
            String email,
            @JsonProperty("email_verified") Boolean emailVerified
    ) {
    }

}
