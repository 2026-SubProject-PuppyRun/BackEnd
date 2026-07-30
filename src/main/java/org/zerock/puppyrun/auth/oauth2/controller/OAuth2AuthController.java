package org.zerock.puppyrun.auth.oauth2.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.auth.DTO.TokenDTO;
import org.zerock.puppyrun.auth.local.controller.response.SignInResponse;
import org.zerock.puppyrun.auth.oauth2.service.SocialAuthService;
import org.zerock.puppyrun.auth.oauth2.controller.request.OAuth2SignInRequest;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;

/**
 * 소셜 로그인 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth2")
public class OAuth2AuthController {
    private final SocialAuthService socialAuthService;

    /**
     * 소셜 제공자의 인가 코드로 로그인하고 PuppyRun 토큰을 발급합니다.
     *
     * @param request 인가 코드와 리다이렉트 URL
     * @param social  소셜 로그인 제공자 이름
     * @return PuppyRun 액세스 토큰과 리프레시 토큰
     */
    @PostMapping("/{social}/sign-in")
    public ResponseEntity<SignInResponse> socialSignIn(
            @Valid @RequestBody OAuth2SignInRequest request,
            @PathVariable String social
    ) {
        SocialProvider socialProvider = SocialProvider.from(social);
        TokenDTO tokenDTO = socialAuthService.signIn(request, socialProvider);

        SignInResponse response = SignInResponse.builder()
                .accessToken(tokenDTO.accessToken())
                .refreshToken(tokenDTO.refreshToken())
                .build();

        return ResponseEntity.ok(response);
    }
}
