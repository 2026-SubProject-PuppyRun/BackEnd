package org.zerock.puppyrun.auth.oauth2.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2Client;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;
import org.zerock.puppyrun.auth.oauth2.exception.OAuth2AuthenticationException;
import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ExternalApiParsingException;
import reactor.core.publisher.Mono;

/**
 * Kakao OAuth 로그인 전략입니다.
 */
@Component
public class KakaoOAuth2Strategy implements OAuth2ProviderStrategy {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;

    public KakaoOAuth2Strategy(
            WebClient webClient,
            @Value("${social-oauth2.kakao.client-id}") String clientId,
            @Value("${social-oauth2.kakao.client-secret}") String clientSecret
    ) {
        this.webClient = webClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public String exchangeAccessToken(OAuth2Client client) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", requireText(client.authorizationCode(), "Kakao 인가 코드는 필수입니다."));
        formData.add("client_id", requireConfig(clientId, "Kakao OAuth client-id 설정이 필요합니다."));
        formData.add("client_secret", requireConfig(clientSecret, "Kakao OAuth client-secret 설정이 필요합니다."));
        formData.add("redirect_uri", requireText(client.redirectUrl(), "Kakao 리다이렉트 URL은 필수입니다."));

        KakaoTokenResponse token = webClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.releaseBody()
                                .then(Mono.error(new OAuth2AuthenticationException(
                                        "Kakao 인가 코드가 유효하지 않습니다."
                                )))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.releaseBody()
                                .then(Mono.error(new ExternalApiParsingException(
                                        "Kakao 토큰 API 호출에 실패했습니다."
                                )))
                )
                .bodyToMono(KakaoTokenResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .onErrorMap(
                        error -> !(error instanceof BusinessException),
                        error -> new ExternalApiParsingException("Kakao 토큰 API와 통신할 수 없습니다.", error)
                )
                .block();
        if (token == null) {
            throw new ExternalApiParsingException("Kakao 토큰 응답이 비어있습니다.");
        }
        return requireText(token.accessToken(), "Kakao 액세스 토큰이 비어있습니다.");
    }

    @Override
    public OAuth2UserProfile fetchUserProfile(String accessToken) {
        KakaoUserResponse user = webClient.get()
                .uri(USER_INFO_URL)
                .headers(headers -> headers.setBearerAuth(requireText(
                        accessToken,
                        "Kakao 액세스 토큰이 비어있습니다."
                )))
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.releaseBody()
                                .then(Mono.error(new OAuth2AuthenticationException(
                                        "Kakao 액세스 토큰이 유효하지 않습니다."
                                )))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.releaseBody()
                                .then(Mono.error(new ExternalApiParsingException(
                                        "Kakao 사용자 정보 API 호출에 실패했습니다."
                                )))
                )
                .bodyToMono(KakaoUserResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .onErrorMap(
                        error -> !(error instanceof BusinessException),
                        error -> new ExternalApiParsingException("Kakao 사용자 정보 API와 통신할 수 없습니다.", error)
                )
                .block();

        if (user == null) {
            throw new ExternalApiParsingException("Kakao 사용자 정보 응답이 비어있습니다.");
        }

        KakaoAccount account = user.kakaoAccount();
        String providerUserId = user.id() == null ? null : user.id().toString();
        String email = account == null ? null : account.email();
        String nickName = account == null || account.profile() == null
                ? null
                : account.profile().nickName();
        return new OAuth2UserProfile(
                getProvider(),
                providerUserId,
                email,
                nickName);
    }

    private record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    private record KakaoUserResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
    }

    private record KakaoAccount(
            String email,
            KakaoProfile profile
    ) {
    }

    private record KakaoProfile(@JsonProperty("nickname") String nickName) {
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new OAuth2AuthenticationException(message);
        }
        return value;
    }

    private String requireConfig(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ExternalApiParsingException(message);
        }
        return value;
    }
}
