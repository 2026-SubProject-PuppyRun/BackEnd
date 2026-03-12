package org.zerock.puppyrun.pet.entity;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.zerock.puppyrun.common.exception.DataIntegrityException;

@Getter
@RequiredArgsConstructor
public enum PetBadge {

    BEGINNER("000", 0, 10_000),
    EXPLORER("001", 10_000, 50_000), // 10km
    RUNNER("002", 50_000, 100_000), // 50km
    MARATHONER("003", 100_000, 500_000), // 100km
    LEGEND("004", 500_000, Integer.MAX_VALUE); // 500km

    private final String code;
    private final int requiredDistance; // 획득 기준 거리 (미터)
    private final int nextRequiredDistance; // 다음 획득 거리

    // 클래스 로드 시점에 단 한 번만 실행되어 미리 정렬된 리스트를 캐싱
    private static final List<PetBadge> BADGES_BY_DISTANCE_DESC =
            Arrays.stream(values())
                    .sorted(Comparator.comparingInt(PetBadge::getRequiredDistance).reversed())
                    .toList();

    /**
     * 문자열 코드값을 입력받아 해당하는 PetBadge Enum 상수를 반환합니다.
     *
     * @param code 뱃지 코드 (e.g., "001", "002")
     * @return 코드에 해당하는 PetBadge 상수
     * @throws DataIntegrityException 정의되지 않은 코드일 경우 예외 발생
     */
    public static PetBadge fromCode(String code) {
        return Arrays.stream(values())
                .filter(badge -> badge.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new DataIntegrityException("잘못된 뱃지 코드입니다: " + code));
    }


    /**
     * 누적 거리에 따라 획득할 수 있는 가장 높은 등급의 뱃지를 반환합니다.
     *
     * @param distance 누적 산책 거리 (미터)
     * @return 거리에 맞는 가장 높은 등급의 뱃지
     */
    public static PetBadge getBadgeByDistance(double distance) {
        for (PetBadge badge : BADGES_BY_DISTANCE_DESC) {
            if (distance >= badge.getRequiredDistance()) {
                return badge;
            }
        }
        return BEGINNER;
    }
}
