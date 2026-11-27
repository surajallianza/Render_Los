package com.los.bank.lookup.repository;

import com.los.bank.lookup.entity.Lookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LookupRepository extends JpaRepository<Lookup, Long> {

    boolean existsByCode(String code);

    Optional<Lookup> findByCode(String code);

    @Query("SELECT DISTINCT l FROM Lookup l " +
           "LEFT JOIN FETCH l.subLookups s " +
           "WHERE (:status = 'ALL' " +
           "   OR (:status = 'ACTIVE' AND l.isActive = true) " +
           "   OR (:status = 'INACTIVE' AND l.isActive = false)) " +
           "AND (:search IS NULL OR :search = '' " +
           "   OR LOWER(l.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY l.id ASC")
    List<Lookup> searchLookupsWithSubLookups(@Param("search") String search, @Param("status") String status);

    @Query("SELECT l FROM Lookup l WHERE " +
           "LOWER(TRIM(l.description)) = LOWER(TRIM(:name)) " +
           "OR REPLACE(LOWER(TRIM(l.description)), ' ', '') = LOWER(REPLACE(TRIM(:name), ' ', '')) " +
           "OR LOWER(TRIM(l.code)) = LOWER(TRIM(:name))")
    List<Lookup> findByNormalizedName(@Param("name") String name);

    @Query("SELECT l FROM Lookup l " +
           "WHERE (:status = 'ALL' " +
           "   OR (:status = 'ACTIVE' AND l.isActive = true) " +
           "   OR (:status = 'INACTIVE' AND l.isActive = false)) " +
           "AND (:search IS NULL OR :search = '' " +
           "   OR LOWER(l.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY l.id ASC")
    List<Lookup> searchLookups(@Param("search") String search, @Param("status") String status);
}
