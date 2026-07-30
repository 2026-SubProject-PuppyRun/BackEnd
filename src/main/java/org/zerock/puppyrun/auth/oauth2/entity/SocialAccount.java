package org.zerock.puppyrun.auth.oauth2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.zerock.puppyrun.common.entity.BaseEntity;
import org.zerock.puppyrun.member.entity.Member;

/**
 * 회원과 소셜 로그인 제공자의 계정을 연결하는 엔티티입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "social_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_account_provider_user_id",
                columnNames = {"provider", "provider_user_id"}
        )
)
public class SocialAccount extends BaseEntity {

    @Id
    private UUID id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;


    @Builder
    public SocialAccount(
            UUID id,
            Member member,
            SocialProvider provider,
            String providerUserId
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.member = member;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

}
