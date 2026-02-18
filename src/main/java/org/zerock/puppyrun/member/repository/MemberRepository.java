package org.zerock.puppyrun.member.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.UserNotFoundException;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    default Member findByIdOrThrow(UUID id) {
        return findById(id)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));
    }

    boolean existsByNickName(String nickName);

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);
}
