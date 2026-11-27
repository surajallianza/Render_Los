package com.los.bank.lookup.repository;

import com.los.bank.lookup.entity.SubLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubLookupRepository extends JpaRepository<SubLookup, Long> {

    boolean existsByLookupTypeCodeAndSubTypeCode(String lookupTypeCode, String subTypeCode);

    Optional<SubLookup> findByLookupTypeCodeAndSubTypeCode(String lookupTypeCode, String subTypeCode);

    @Query("SELECT s FROM SubLookup s " +
           "JOIN FETCH s.lookup l " +
           "WHERE s.lookupTypeCode = :lookupTypeCode AND s.subTypeCode = :subTypeCode")
    Optional<SubLookup> findByLookupTypeCodeAndSubTypeCodeWithLookup(
            @Param("lookupTypeCode") String lookupTypeCode,
            @Param("subTypeCode") String subTypeCode);

    List<SubLookup> findByLookupTypeCodeOrderByDisplayOrderAscSubTypeCodeAsc(String lookupTypeCode);

    List<SubLookup> findByLookupTypeCodeAndIsActiveTrueOrderByDisplayOrderAscSubTypeCodeAsc(String lookupTypeCode);

    List<SubLookup> findByLookupIdOrderByDisplayOrderAscSubTypeCodeAsc(Long lookupId);

    List<SubLookup> findByLookupIdAndIsActiveTrueOrderByDisplayOrderAscSubTypeCodeAsc(Long lookupId);

    @Query("SELECT COALESCE(MAX(s.displayOrder), 0) FROM SubLookup s WHERE s.lookupTypeCode = :lookupTypeCode")
    Integer findMaxDisplayOrderByLookupTypeCode(@Param("lookupTypeCode") String lookupTypeCode);

    List<SubLookup> findByLookupTypeCodeInOrderByLookupTypeCodeAscDisplayOrderAscSubTypeCodeAsc(
            Collection<String> lookupTypeCodes);

    List<SubLookup> findByLookupTypeCodeInAndIsActiveTrueOrderByLookupTypeCodeAscDisplayOrderAscSubTypeCodeAsc(
            Collection<String> lookupTypeCodes);
}
