package org.zerock.puppyrun.tracking.entity;

/**
 * 경로 상의 단일 좌표와 시간 정보를 저장하는 순수 Record
 */
public record RoutePoint(
        Double lat,
        Double lng,
        Integer time
) {
}
