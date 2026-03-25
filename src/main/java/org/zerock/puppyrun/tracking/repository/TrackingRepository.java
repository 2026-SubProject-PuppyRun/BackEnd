package org.zerock.puppyrun.tracking.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.tracking.entity.Tracking;

@Repository
public interface TrackingRepository extends JpaRepository<Tracking, UUID>, TrackingRepoCustom {

    /**
     * 산책 기록을 조회하고, 요청한 사용자의 소유인지 검증합니다.
     *
     * @param trackingId 조회할 산책 기록 ID
     * @param memberId   요청한 사용자의 ID
     * @return 검증이 완료된 Tracking 엔티티
     */
    default Tracking findByIdAndVerifyOwnership(UUID trackingId, UUID memberId) {
        Tracking tracking = findById(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 산책 기록을 찾을 수 없습니다."));

        if (tracking.isNotOwner(memberId)) {
            throw new UserForbiddenException("해당 산책 기록에 대한 권한이 없습니다.");
        }

        return tracking;
    }

    List<Tracking> findAllByMemberId(UUID memberId);

}
