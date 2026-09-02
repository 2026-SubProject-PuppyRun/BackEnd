package org.zerock.puppyrun.terms.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.puppyrun.terms.entity.TermsAgreement;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, UUID> {

    List<TermsAgreement> findAllByMemberId(UUID memberId);
}
