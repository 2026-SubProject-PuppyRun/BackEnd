package org.zerock.puppyrun.tracking.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.tracking.entity.TrackingImage;

@Repository
public interface TrackingImageRepository extends JpaRepository<TrackingImage, UUID> {

    @Query("""
            select image.imageUrl
            from TrackingImage image
            join image.tracking tracking
            where tracking.member.id = :memberId
            """)
    List<String> findImageUrlsByMemberId(@Param("memberId") UUID memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from TrackingImage image
            where image.tracking.id in (
                select tracking.id from Tracking tracking where tracking.member.id = :memberId
            )
            """)
    void deleteByTrackingMemberId(@Param("memberId") UUID memberId);
}
