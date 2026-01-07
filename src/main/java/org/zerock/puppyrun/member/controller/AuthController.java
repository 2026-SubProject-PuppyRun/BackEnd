package org.zerock.puppyrun.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.member.DTO.TokenDTO;
import org.zerock.puppyrun.member.controller.request.SignInRequest;
import org.zerock.puppyrun.member.controller.request.SignUpRequest;
import org.zerock.puppyrun.member.controller.response.CheckResponse;
import org.zerock.puppyrun.member.controller.response.SignInResponse;
import org.zerock.puppyrun.member.controller.response.SignUpResponse;
import org.zerock.puppyrun.member.service.AuthService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    // 로그인
    @PostMapping("/sign-in")
    public ResponseEntity<SignInResponse> signIn(@Valid @RequestBody SignInRequest request) {

        TokenDTO tokenDTO = authService.signIn(request);

        SignInResponse response = SignInResponse.builder()
                .accessToken(tokenDTO.accessToken())
                .refreshToken(tokenDTO.refreshToken())
                .build();

        return ResponseEntity.ok(response);
    }

    // 회원가입
    @PostMapping("/sign-up")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {

        MemberDTO memberDTO = authService.registrarMember(request);

        TokenDTO tokenDTO = authService.crateToken(memberDTO);

        SignUpResponse response = SignUpResponse.builder()
                .accessToken(tokenDTO.accessToken())
                .refreshToken(tokenDTO.refreshToken())
                .email(memberDTO.email())
                .nickName(memberDTO.nickName())
                .build();
        return ResponseEntity.ok(response);
    }

    // 이메일 중복 체크
    @GetMapping("/check-email")
    public ResponseEntity<CheckResponse> checkEmail(@RequestParam String email) {

        CheckResponse response = CheckResponse.builder()
                .object(email)
                .isExists(authService.isExistsByEmail(email))
                .build();
        return ResponseEntity.ok(response);
    }

    // 닉네임 중복 체크
    @GetMapping("/check-nickname")
    public ResponseEntity<CheckResponse> checkNickname(@RequestParam String nickname) {
        CheckResponse response = CheckResponse.builder()
                .object(nickname)
                .isExists(authService.isExistsByNickname(nickname))
                .build();
        return ResponseEntity.ok(response);
    }

}
