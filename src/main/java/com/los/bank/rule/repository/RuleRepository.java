package com.los.bank.rule.repository;

import com.los.bank.rule.entity.Rule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {

    List<Rule> findByStatusIgnoreCase(String status);

    List<Rule> findByParamIgnoreCase(String param);

    List<Rule> findByParamIgnoreCaseAndStatusIgnoreCase(String param, String status);

    @Query("SELECT r FROM Rule r WHERE " +
           "(:status IS NULL OR LOWER(r.status) = LOWER(:status)) AND " +
           "(:param IS NULL OR LOWER(r.param) LIKE LOWER(CONCAT('%', :param, '%')))")
    Page<Rule> findWithFilters(@Param("status") String status,
                               @Param("param") String param,
                               Pageable pageable);

    boolean existsByParamIgnoreCaseAndComparisonOperatorIgnoreCaseAndFromValueAndToValueAndValAndStatusIgnoreCase(
            String param, String comparisonOperator, String fromValue, String toValue, String val, String status);
}
