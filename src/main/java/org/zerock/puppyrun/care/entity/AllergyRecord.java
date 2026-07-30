package org.zerock.puppyrun.care.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
@Table(name = "allergy_record")
public class AllergyRecord extends BaseEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Pet pet;

    @Column(name = "allergen_name", nullable = false, length = 100)
    private String allergenName;

    @Column(length = 255)
    private String symptom;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AllergySeverity severity;

    @Column(name = "identified_at")
    private LocalDate identifiedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Builder
    public AllergyRecord(
            UUID id,
            Pet pet,
            String allergenName,
            String symptom,
            AllergySeverity severity,
            LocalDate identifiedAt,
            Boolean isActive,
            String memo
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.pet = pet;
        this.allergenName = allergenName;
        this.symptom = symptom;
        this.severity = severity;
        this.identifiedAt = identifiedAt;
        this.isActive = isActive != null ? isActive : Boolean.TRUE;
        this.memo = memo;
    }

    public void update(
            String allergenName,
            String symptom,
            AllergySeverity severity,
            LocalDate identifiedAt,
            Boolean isActive,
            String memo
    ) {
        this.allergenName = allergenName;
        this.symptom = symptom;
        this.severity = severity;
        this.identifiedAt = identifiedAt;
        this.isActive = isActive;
        this.memo = memo;
    }
}
