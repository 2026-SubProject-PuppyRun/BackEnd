package org.zerock.puppyrun.pet.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetStatistics {

    /**
     * 특정 펫의 누적 산책 거리(미터)를 조회합니다.
     */
    public int getTotalWalkedDistance(UUID petId) {
        // Todo: 추후 통계 개발 예정
        return 0;
    }

    /**
     * 특정 펫의 몸무게를 저장 합니다
     */
    public void savePetWeightLog(UUID petId, double weight) {
        // Todo: 무게 조정 시 무게 log 등록 로직 추가
    }

}
