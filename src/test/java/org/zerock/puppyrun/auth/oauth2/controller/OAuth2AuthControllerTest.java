package org.zerock.puppyrun.auth.oauth2.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.zerock.puppyrun.auth.DTO.TokenDTO;
import org.zerock.puppyrun.auth.oauth2.controller.request.OAuth2SignInRequest;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.auth.oauth2.service.SocialAuthService;
import org.zerock.puppyrun.common.auth.security.JwtAuthenticationFilter;

@WebMvcTest(OAuth2AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("소셜 로그인 API")
class OAuth2AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SocialAuthService socialAuthService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("제공자 액세스 토큰만 전달하면 소셜 로그인을 처리한다")
    void socialSignIn() throws Exception {
        // given
        OAuth2SignInRequest request = new OAuth2SignInRequest("provider-access-token");
        TokenDTO token = TokenDTO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
        when(socialAuthService.signIn(request, SocialProvider.KAKAO)).thenReturn(token);

        // when & then
        mockMvc.perform(post("/api/oauth2/kakao/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider_access_token":"provider-access-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.refresh_token").value("refresh-token"));

        verify(socialAuthService).signIn(request, SocialProvider.KAKAO);
    }
}
