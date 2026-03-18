package org.zerock.puppyrun.diary.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.diary.controller.request.RegisterDiaryRequest;
import org.zerock.puppyrun.diary.controller.request.UpdateDiaryRequest;
import org.zerock.puppyrun.diary.controller.response.DiaryResponse;
import org.zerock.puppyrun.diary.service.DiaryService;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {
    private final DiaryService diaryService;

    /**
     * 일기 작성
     */
    @PostMapping
    public ResponseEntity<DiaryResponse> registerDiary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestPart("request") @Valid RegisterDiaryRequest request,
            @RequestPart("images") List<MultipartFile> images
    ) {
        UUID memberId = userPrincipal.id(); // 인증된 사용자 ID 가져오기

        DiaryResponse response = diaryService.registerDiary(memberId, request, images);

        return ResponseEntity.ok(response);
    }

    /**
     * 일기 수정
     */
    @PutMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> updateDiary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID diaryId,
            @RequestBody @Valid UpdateDiaryRequest request
    ) {
        UUID memberId = userPrincipal.id(); // 인증된 사용자 ID 가져오기

        DiaryResponse response = diaryService.updateDiary(memberId, diaryId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 일기 삭제
     */
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID diaryId
    ) {
        UUID memberId = userPrincipal.id(); // 인증된 사용자 ID 가져오기

        diaryService.deleteDiary(memberId, diaryId);

        return ResponseEntity.ok().build();
    }

    /**
     * 일기 상세 조회
     */
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID diaryId
    ) {
        UUID memberId = userPrincipal.id(); // 인증된 사용자 ID 가져오기

        DiaryResponse response = diaryService.getDiary(memberId, diaryId);

        return ResponseEntity.ok(response);
    }

}
