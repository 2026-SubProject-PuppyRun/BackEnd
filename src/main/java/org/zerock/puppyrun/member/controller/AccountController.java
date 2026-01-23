package org.zerock.puppyrun.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.member.controller.request.ChangeNicknameRequest;
import org.zerock.puppyrun.member.controller.request.ChangePasswordRequest;
import org.zerock.puppyrun.member.controller.response.AccountResponse;
import org.zerock.puppyrun.member.service.MemberService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountController {
    private final MemberService memberService;

    // 유저 정보 조회
    @GetMapping("")
    public ResponseEntity<AccountResponse> accountView(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        MemberDTO memberDTO = memberService.memberInformationView(userPrincipal.id());
        String maskEmail = memberService.maskEmail(memberDTO.email());

        AccountResponse response = AccountResponse.builder()
                .email(maskEmail)
                .nickName(memberDTO.nickName())
                .UserRole(memberDTO.userRole().toString())
                .build();
        return ResponseEntity.ok(response);
    }

    // 비밀번호 변경
    @PostMapping("/change/password")
    public ResponseEntity<AccountResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        MemberDTO memberDTO = memberService.passwordChange(userPrincipal.id(), request.oldPassword(),
                request.newPassword());
        String maskEmail = memberService.maskEmail(memberDTO.email());

        AccountResponse response = AccountResponse.builder()
                .email(maskEmail)
                .nickName(memberDTO.nickName())
                .UserRole(memberDTO.userRole().toString())
                .build();
        return ResponseEntity.ok(response);
    }

    // 닉네임 변경
    @PostMapping("/change/nickname")
    public ResponseEntity<AccountResponse> changeNickName(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChangeNicknameRequest request
    ) {
        MemberDTO memberDTO = memberService.nickNameChange(userPrincipal.id(), request.nickName());
        String maskEmail = memberService.maskEmail(memberDTO.email());

        AccountResponse response = AccountResponse.builder()
                .email(maskEmail)
                .nickName(memberDTO.nickName())
                .UserRole(memberDTO.userRole().toString())
                .build();
        return ResponseEntity.ok(response);
    }

    // 계정 삭제
    // @DeleteMapping("/")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // TODO 추후 기능 개발 예정
        return ResponseEntity.ok("계정 삭제 완료");
    }
}
