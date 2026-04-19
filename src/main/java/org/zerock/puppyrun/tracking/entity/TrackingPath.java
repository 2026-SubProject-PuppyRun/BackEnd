package org.zerock.puppyrun.tracking.entity;

// 경로 좌표를 저장할 클래스
public record TrackingPath(
        Double lat,  // 위도
        Double lng,  // 경도
        Integer time // 시작 시간으로부터의 경과 시간
) {
}
