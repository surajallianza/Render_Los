package com.los.bank.rule.repository;

import com.los.bank.rule.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {

    /** Rules used by an eligibility check on {@code date}. */
    @Query("SELECT r FROM Rule r WHERE r.status = 'ACTIVE' AND r.effectiveDate <= :date ORDER BY r.paramLookupId, r.id")
    List<Rule> findRulesInForce(@Param("date") LocalDate date);

    /** Active rules of one parameter, for the overlap check. */
    List<Rule> findByParamLookupIdAndStatus(Long paramLookupId, String status);
}
