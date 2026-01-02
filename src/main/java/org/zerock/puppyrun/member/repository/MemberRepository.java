package org.zerock.puppyrun.member.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.member.entity.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    boolean existsByNickName(String nickName);

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);
}
