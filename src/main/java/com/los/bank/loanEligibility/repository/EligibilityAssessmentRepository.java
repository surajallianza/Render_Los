package com.los.bank.loanEligibility.repository;

import com.los.bank.loanEligibility.entity.EligibilityAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EligibilityAssessmentRepository extends JpaRepository<EligibilityAssessment, Long> {

    Page<EligibilityAssessment> findByLeadIdOrderByCreatedAtDesc(String leadId, Pageable pageable);

    Page<EligibilityAssessment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
