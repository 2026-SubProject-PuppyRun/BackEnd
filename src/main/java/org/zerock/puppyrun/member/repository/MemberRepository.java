package org.zerock.puppyrun.member.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.UserNotFoundException;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    default Member findByIdOrThrow(UUID id) {
        return findById(id)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByNickName(String nickName);

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);
}
