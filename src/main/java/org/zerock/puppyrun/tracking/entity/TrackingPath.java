package org.zerock.puppyrun.tracking.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 경로 좌표를 저장할 클래스
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TrackingPath {
    private Double lat;  // 위도
    private Double lng;  // 경도
    private Integer time; // 시작 시간으로부터의 경과 시간
}
