package org.zerock.puppyrun.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.zerock.puppyrun.common.entity.BaseTimeEntity;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.member.DTO.MemberDTO;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member extends BaseTimeEntity {
    // 기본 프로필 이미지 경로를 상수로 정의
    private static final String DEFAULT_PROFILE_IMAGE = "/images/default/profile.png";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nick_name", nullable = false, unique = true, length = 50)
    private String nickName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "profile_image")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Builder
    public Member(String nickName, String email, String password) {
        this.nickName = nickName;
        this.email = email;
        this.password = password;
        this.role = UserRole.USER; // 생성시 USER 할당
        this.status = Status.ACTIVE; // 생성시 ACTIVE 할당
        this.profileImage = DEFAULT_PROFILE_IMAGE; // 생성시 기본 이미지 경로 할당
    }

    public void setAdmin() {
        this.role = UserRole.ADMIN;
    }

    public void setDeactivate() {
        this.status = Status.DEACTIVATE;
    }

    public void setActive() {
        this.status = Status.ACTIVE;
    }

    public void updatePassword(String encryptedPassword) {
        this.password = encryptedPassword;
    }

    public void updateProfileImage(String profileImage) {
        // TODO 기능 개발 예정
        this.profileImage = profileImage;
    }

    public void updateNickName(String newNickName) {
        if (nickName.equals(newNickName)) {
            throw new InvalidValueException("기존 닉네임과 동일합니다.");
        }
        this.nickName = newNickName;
    }

    public MemberDTO toDto() {
        return MemberDTO.builder()
                .id(this.id)
                .nickName(this.nickName)
                .email(this.email)
                .userRole(this.role)
                .status(this.status)
                .profileImage(this.profileImage)
                .build();
    }
}
