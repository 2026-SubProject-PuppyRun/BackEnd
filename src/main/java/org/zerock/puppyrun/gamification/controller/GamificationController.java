package org.zerock.puppyrun.gamification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.gamification.controller.response.GamificationResponse;
import org.zerock.puppyrun.gamification.service.GamificationQueryService;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {
    private final GamificationQueryService gamificationQueryService;

    @GetMapping
    public ResponseEntity<GamificationResponse> getGamification(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        GamificationResponse response = gamificationQueryService.getGamification(userPrincipal);
        return ResponseEntity.ok(response);
    }
}
