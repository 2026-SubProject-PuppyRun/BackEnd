package org.zerock.puppyrun.tracking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tracking_images",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tracking_images_tracking_id_image_order",
                columnNames = {"tracking_id", "image_order"}
        )
)
public class TrackingImage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tracking_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tracking tracking;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "image_order", nullable = false)
    private Integer imageOrder;

    @Builder
    public TrackingImage(UUID id, Tracking tracking, String imageUrl, Integer imageOrder) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tracking = tracking;
        this.imageUrl = imageUrl;
        this.imageOrder = imageOrder;
    }

    void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
