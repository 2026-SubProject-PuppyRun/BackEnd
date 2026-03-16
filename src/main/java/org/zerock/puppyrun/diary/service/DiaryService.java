package org.zerock.puppyrun.diary.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.diary.DTO.UpdateDiaryDTO;
import org.zerock.puppyrun.diary.controller.request.RegisterDiaryRequest;
import org.zerock.puppyrun.diary.controller.response.DiaryResponse;
import org.zerock.puppyrun.diary.entity.Diary;
import org.zerock.puppyrun.diary.repository.DiaryRepository;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DiaryService {
    private final TrackingRepository trackingRepository;
    private final DiaryRepository diaryRepository;
    private final MemberRepository memberRepository;

    // 일기 작성
    @Transactional
    public DiaryResponse registerDiary(UUID memberId, RegisterDiaryRequest request, List<MultipartFile> images) {

        if (diaryRepository.existsByTrackingId(request.trackingId())) {
            throw new InvalidValueException("이미 해당 산책 기록에 대한 일기가 존재합니다.");
        }

        Tracking tracking = trackingRepository.findById(request.trackingId())
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 산책 기록입니다"));

        List<String> imagesUrl = List.of(); // Todo: s3 추가 예정

        SkyType skyType = SkyType.fromCode(request.weather().sky());  // 날씨 코드 변환
        PrecipitationType precipitationType = PrecipitationType.fromCode(request.weather().pty()); // 날씨 코드 변환
        String temp = request.weather().temp();

        Diary diary = Diary.builder()
                .sky(skyType)
                .pty(precipitationType)
                .temp(temp)
                .title(request.title())
                .content(request.content())
                .writingTime(request.writingTime())
                .member(memberRepository.findByIdOrThrow(memberId))
                .tracking(tracking)
                .images(imagesUrl)
                .build();

        Diary savedDiary = diaryRepository.save(diary);

        return DiaryResponse.of(savedDiary);
    }

    // 일기 수정
    @Transactional
    public DiaryResponse updateDiary(UUID memberId, UUID diaryId, RegisterDiaryRequest request) {
        Diary diary = findDiaryWithOwnershipCheck(diaryId, memberId);

        SkyType skyType = SkyType.fromCode(request.weather().sky());  // 날씨 코드 변환
        PrecipitationType precipitationType = PrecipitationType.fromCode(request.weather().pty()); // 날씨 코드 변환
        String temp = request.weather().temp();

        List<String> imagesUrl = List.of(); // Todo: s3 추가 예정

        UpdateDiaryDTO updateDiaryDTO = UpdateDiaryDTO.builder()
                .title(request.title())
                .content(request.content())
                .images(imagesUrl)
                .writingTime(request.writingTime())
                .pty(precipitationType)
                .sky(skyType)
                .temp(temp)
                .build();

        diary.update(updateDiaryDTO);
        return DiaryResponse.of(diary);
    }

    // 일기 삭제
    @Transactional
    public void deleteDiary(UUID memberId, UUID diaryId) {
        Diary diary = findDiaryWithOwnershipCheck(diaryId, memberId);
        diaryRepository.delete(diary);
    }

    // 일기 조회
    public DiaryResponse getDiary(UUID memberId, UUID diaryId) {
        Diary diary = findDiaryWithOwnershipCheck(diaryId, memberId);
        return DiaryResponse.of(diary);
    }


    /**
     * 일기 조회 및 소유권 검증
     */
    private Diary findDiaryWithOwnershipCheck(UUID diaryId, UUID memberId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 일기입니다."));

        if (diary.isNotOwner(memberId)) {
            throw new UserForbiddenException("해당 일기에 대한 권한이 없습니다.");
        }
        return diary;
    }

}
