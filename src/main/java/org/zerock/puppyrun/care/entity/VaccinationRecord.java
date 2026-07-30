package org.zerock.puppyrun.care.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "vaccination_record")
public class VaccinationRecord extends BaseEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Pet pet;

    @Column(name = "vaccine_name", nullable = false, length = 100)
    private String vaccineName;

    @Column(name = "vaccinated_at", nullable = false)
    private LocalDate vaccinatedAt;

    @Column(name = "next_vaccination_date")
    private LocalDate nextVaccinationDate;

    @Column(name = "hospital_name", length = 100)
    private String hospitalName;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Builder
    public VaccinationRecord(
            UUID id,
            Pet pet,
            String vaccineName,
            LocalDate vaccinatedAt,
            LocalDate nextVaccinationDate,
            String hospitalName,
            String memo
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.pet = pet;
        this.vaccineName = vaccineName;
        this.vaccinatedAt = vaccinatedAt;
        this.nextVaccinationDate = nextVaccinationDate;
        this.hospitalName = hospitalName;
        this.memo = memo;
    }

    public void update(
            String vaccineName,
            LocalDate vaccinatedAt,
            LocalDate nextVaccinationDate,
            String hospitalName,
            String memo
    ) {
        this.vaccineName = vaccineName;
        this.vaccinatedAt = vaccinatedAt;
        this.nextVaccinationDate = nextVaccinationDate;
        this.hospitalName = hospitalName;
        this.memo = memo;
    }
}
