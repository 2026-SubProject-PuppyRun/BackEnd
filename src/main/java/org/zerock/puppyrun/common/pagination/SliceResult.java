package org.zerock.puppyrun.common.pagination;

import java.util.List;

/**
 * 페이지 또는 커서 기반 조회 결과와 다음 조회 가능 여부를 함께 전달합니다.
 *
 * @param content 조회 데이터
 * @param hasNext 다음 조회 가능 여부
 * @param <T>     조회 데이터 타입
 */
public record SliceResult<T>(List<T> content, boolean hasNext) {

    public SliceResult {
        content = List.copyOf(content);
    }
}
