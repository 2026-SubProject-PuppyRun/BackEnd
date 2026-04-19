package org.zerock.puppyrun.care.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.zerock.puppyrun.common.entity.BaseEntity;
import org.zerock.puppyrun.pet.entity.Pet;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "medication_record")
public class MedicationRecord extends BaseEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Pet pet;

    @Column(name = "medication_name", nullable = false, length = 100)
    private String medicationName;

    @Column(name = "administered_at", nullable = false)
    private LocalDateTime administeredAt;

    @Column(name = "dose_amount", nullable = false)
    private Double doseAmount;

    @Column(name = "dose_unit", nullable = false, length = 30)
    private String doseUnit;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Builder
    public MedicationRecord(
            UUID id,
            Pet pet,
            String medicationName,
            LocalDateTime administeredAt,
            Double doseAmount,
            String doseUnit,
            String memo
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.pet = pet;
        this.medicationName = medicationName;
        this.administeredAt = administeredAt;
        this.doseAmount = doseAmount;
        this.doseUnit = doseUnit;
        this.memo = memo;
    }

    public void update(
            String medicationName,
            LocalDateTime administeredAt,
            Double doseAmount,
            String doseUnit,
            String memo
    ) {
        this.medicationName = medicationName;
        this.administeredAt = administeredAt;
        this.doseAmount = doseAmount;
        this.doseUnit = doseUnit;
        this.memo = memo;
    }
}
