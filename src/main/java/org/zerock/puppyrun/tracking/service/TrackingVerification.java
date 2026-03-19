package org.zerock.puppyrun.tracking.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@Component
@RequiredArgsConstructor
public class TrackingVerification {
    private final TrackingRepository trackingRepository;

    /**
     * 산책 기록을 조회하고, 요청한 사용자의 소유인지 검증합니다.
     */
    public Tracking ownershipCheck(UUID memberId, UUID trackingId) {
        Tracking tracking = trackingRepository.findById(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 산책 기록을 찾을 수 없습니다."));

        if (tracking.isNotOwner(memberId)) {
            throw new UserForbiddenException("해당 산책 기록에 대한 권한이 없습니다.");
        }

        return tracking;
    }
}
