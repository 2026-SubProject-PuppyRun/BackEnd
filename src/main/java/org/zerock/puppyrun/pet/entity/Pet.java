package org.zerock.puppyrun.pet.entity;

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
import org.zerock.puppyrun.common.entity.BaseEntity;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.pet.DTO.UpdatePetDTO;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pet")
public class Pet extends BaseEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "birth_year", nullable = false)
    private LocalDate birthYear; // 출생년도

    @Enumerated(EnumType.STRING)
    @Column(name = "badge")
    private PetBadge badge;

    @Enumerated(EnumType.STRING)
    @Column(name = "breed")
    private Breed breed;

    @Column(name = "profile_image_url", length = 1000)
    private String profileImageUrl;

    @Column(name = "color")
    private String color;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "is_neutered", nullable = false)
    private Boolean isNeutered;

    // length = 1로 설정하여 (M/F)만 들어가도록 제한
    @Column(name = "gender", nullable = false, length = 1)
    private String gender;

    @Builder
    public Pet(UUID id, Member member, String name, LocalDate birthYear, Breed breed, String color, Double weight,
               boolean isNeutered, String gender) {
        this.id = id != null ? id : UUID.randomUUID();
        this.member = member;
        this.name = name;
        this.breed = breed;
        this.birthYear = birthYear;
        this.color = (color == null || color.isBlank()) ? breed.getBasicColorHex() : color;
        this.weight = (weight == null || weight <= 0) ? breed.getAvgWeightMin() : weight;
        this.isNeutered = isNeutered;
        this.gender = gender;
        this.profileImageUrl = null;
        this.badge = PetBadge.BEGINNER;
    }

    public void updateProfile(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updatePet(UpdatePetDTO dto) {
        this.name = dto.name();
        this.color = dto.color();
        this.weight = dto.weight();
        this.birthYear = dto.birthYear();
        this.isNeutered = dto.isNeutered();
        this.gender = dto.gender();
    }


    public void setDefaultProfile() {
        this.profileImageUrl = null;
    }

    public boolean isNotOwner(UUID memberId) {
        return !this.member.getId().equals(memberId);
    }
}
