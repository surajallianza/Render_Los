package com.los.bank.bankmaster;

import com.los.bank.bankmaster.dto.ApprovalLimitRequestDto;
import com.los.bank.bankmaster.dto.ApprovalLimitResponseDto;
import com.los.bank.bankmaster.entity.ApprovalExposureLimit;
import com.los.bank.bankmaster.exception.BankMasterException;
import com.los.bank.bankmaster.repository.ApprovalExposureLimitRepository;
import com.los.bank.bankmaster.service.ApprovalExposureLimitServiceImpl;
import com.los.bank.branch.entity.Branch;
import com.los.bank.branch.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalExposureLimitServiceTest {

    private ApprovalExposureLimitServiceImpl service;
    private MockLimitRepository limitRepository;
    private MockBranchRepository branchRepository;

    @BeforeEach
    void setUp() {
        limitRepository = new MockLimitRepository();
        branchRepository = new MockBranchRepository();
        service = new ApprovalExposureLimitServiceImpl(limitRepository, branchRepository);

        Branch b = Branch.builder().id(201L).name("Mumbai Corporate Branch").code("MUM001").build();
        branchRepository.save(b);
    }

    @Test
    @DisplayName("Should successfully create approval & exposure limit")
    void testCreateApprovalLimit() {
        ApprovalLimitRequestDto req = new ApprovalLimitRequestDto();
        req.setBranchId(201L);
        req.setApprovalAuthority("Branch Manager");
        req.setSinglePartyLimit(new BigDecimal("1000000.00"));
        req.setGroupExposureLimit(new BigDecimal("2500000.00"));
        req.setSanctionMargin(new BigDecimal("20.0000"));
        req.setPenaltyCharge(new BigDecimal("500.00"));
        req.setPenaltyRate(new BigDecimal("2.0000"));

        ApprovalLimitResponseDto res = service.createApprovalLimit(req);

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals("Branch Manager", res.getApprovalAuthority());
        assertEquals("Mumbai Corporate Branch", res.getBranchName());
        assertEquals(new BigDecimal("1000000.00"), res.getSinglePartyLimit());
        assertEquals(new BigDecimal("2500000.00"), res.getGroupExposureLimit());
        assertTrue(res.getIsActive());
    }

    @Test
    @DisplayName("Should reject negative monetary or rate amounts")
    void testNegativeAmountValidation() {
        ApprovalLimitRequestDto req = new ApprovalLimitRequestDto();
        req.setApprovalAuthority("Credit Officer");
        req.setSinglePartyLimit(new BigDecimal("-100.00"));
        req.setGroupExposureLimit(BigDecimal.ZERO);
        req.setSanctionMargin(BigDecimal.ZERO);
        req.setPenaltyCharge(BigDecimal.ZERO);
        req.setPenaltyRate(BigDecimal.ZERO);

        BankMasterException ex = assertThrows(BankMasterException.class, () -> service.createApprovalLimit(req));
        assertTrue(ex.getMessage().contains("must be non-negative"));
    }

    @Test
    @DisplayName("Should reject duplicate active authority for same branch")
    void testRejectDuplicateAuthority() {
        ApprovalLimitRequestDto req = new ApprovalLimitRequestDto();
        req.setBranchId(201L);
        req.setApprovalAuthority("Branch Manager");
        req.setSinglePartyLimit(new BigDecimal("1000000.00"));
        req.setGroupExposureLimit(new BigDecimal("2500000.00"));
        req.setSanctionMargin(new BigDecimal("20.0000"));
        req.setPenaltyCharge(new BigDecimal("500.00"));
        req.setPenaltyRate(new BigDecimal("2.0000"));

        service.createApprovalLimit(req);

        BankMasterException ex = assertThrows(BankMasterException.class, () -> service.createApprovalLimit(req));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Should retrieve and update approval limit")
    void testGetAndUpdateApprovalLimit() {
        ApprovalLimitRequestDto req = new ApprovalLimitRequestDto();
        req.setBranchId(201L);
        req.setApprovalAuthority("Zonal Manager");
        req.setSinglePartyLimit(new BigDecimal("5000000.00"));
        req.setGroupExposureLimit(new BigDecimal("10000000.00"));
        req.setSanctionMargin(new BigDecimal("15.0000"));
        req.setPenaltyCharge(new BigDecimal("1000.00"));
        req.setPenaltyRate(new BigDecimal("1.5000"));

        ApprovalLimitResponseDto created = service.createApprovalLimit(req);
        Long id = created.getId();

        ApprovalLimitResponseDto fetched = service.getApprovalLimitById(id);
        assertEquals(new BigDecimal("5000000.00"), fetched.getSinglePartyLimit());

        // Update
        req.setSinglePartyLimit(new BigDecimal("6000000.00"));
        ApprovalLimitResponseDto updated = service.updateApprovalLimit(id, req);
        assertEquals(new BigDecimal("6000000.00"), updated.getSinglePartyLimit());
    }

    @Test
    @DisplayName("Should soft-delete approval limit")
    void testSoftDeleteApprovalLimit() {
        ApprovalLimitRequestDto req = new ApprovalLimitRequestDto();
        req.setBranchId(201L);
        req.setApprovalAuthority("Credit Committee");
        req.setSinglePartyLimit(new BigDecimal("20000000.00"));
        req.setGroupExposureLimit(new BigDecimal("50000000.00"));
        req.setSanctionMargin(new BigDecimal("10.0000"));
        req.setPenaltyCharge(new BigDecimal("2000.00"));
        req.setPenaltyRate(new BigDecimal("1.0000"));

        ApprovalLimitResponseDto created = service.createApprovalLimit(req);
        Long id = created.getId();

        service.deleteApprovalLimit(id);

        assertThrows(BankMasterException.class, () -> service.getApprovalLimitById(id));
    }

    // =========================================================================
    // IN-MEMORY MOCKS
    // =========================================================================
    private static class MockLimitRepository implements ApprovalExposureLimitRepository {
        private final ConcurrentHashMap<Long, ApprovalExposureLimit> store = new ConcurrentHashMap<>();
        private final AtomicLong seq = new AtomicLong(1);

        @Override
        public ApprovalExposureLimit save(ApprovalExposureLimit entity) {
            if (entity.getId() == null) {
                entity.setId(seq.getAndIncrement());
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public List<ApprovalExposureLimit> findByIsActiveTrueOrderByIdDesc() {
            List<ApprovalExposureLimit> list = new ArrayList<>();
            for (ApprovalExposureLimit a : store.values()) {
                if (Boolean.TRUE.equals(a.getIsActive())) list.add(a);
            }
            return list;
        }

        @Override
        public Optional<ApprovalExposureLimit> findByIdAndIsActiveTrue(Long id) {
            ApprovalExposureLimit a = store.get(id);
            if (a != null && Boolean.TRUE.equals(a.getIsActive())) {
                return Optional.of(a);
            }
            return Optional.empty();
        }

        @Override
        public List<ApprovalExposureLimit> findByBranchIdAndIsActiveTrue(Long branchId) {
            List<ApprovalExposureLimit> list = new ArrayList<>();
            for (ApprovalExposureLimit a : store.values()) {
                if (Boolean.TRUE.equals(a.getIsActive()) && branchId != null && branchId.equals(a.getBranchId())) {
                    list.add(a);
                }
            }
            return list;
        }

        @Override
        public boolean existsActiveAuthority(String authority, Long branchId) {
            return store.values().stream().anyMatch(a ->
                    Boolean.TRUE.equals(a.getIsActive())
                    && authority.equalsIgnoreCase(a.getApprovalAuthority().trim())
                    && (branchId == null && a.getBranchId() == null || branchId != null && branchId.equals(a.getBranchId())));
        }

        @Override
        public boolean existsActiveAuthorityExcludingId(String authority, Long branchId, Long id) {
            return store.values().stream().anyMatch(a ->
                    Boolean.TRUE.equals(a.getIsActive())
                    && !a.getId().equals(id)
                    && authority.equalsIgnoreCase(a.getApprovalAuthority().trim())
                    && (branchId == null && a.getBranchId() == null || branchId != null && branchId.equals(a.getBranchId())));
        }

        @Override public Optional<ApprovalExposureLimit> findById(Long aLong) { return Optional.ofNullable(store.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return store.containsKey(aLong); }
        @Override public <S extends ApprovalExposureLimit> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public List<ApprovalExposureLimit> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<ApprovalExposureLimit> findAllById(Iterable<Long> longs) { return null; }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long aLong) { store.remove(aLong); }
        @Override public void delete(ApprovalExposureLimit entity) { store.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends ApprovalExposureLimit> entities) {}
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override public <S extends ApprovalExposureLimit> S saveAndFlush(S entity) { return null; }
        @Override public <S extends ApprovalExposureLimit> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<ApprovalExposureLimit> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public ApprovalExposureLimit getOne(Long aLong) { return null; }
        @Override public ApprovalExposureLimit getById(Long aLong) { return null; }
        @Override public ApprovalExposureLimit getReferenceById(Long aLong) { return null; }
        @Override public <S extends ApprovalExposureLimit> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends ApprovalExposureLimit> List<S> findAll(Example<S> example) { return null; }
        @Override public <S extends ApprovalExposureLimit> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override public <S extends ApprovalExposureLimit> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends ApprovalExposureLimit> long count(Example<S> example) { return 0; }
        @Override public <S extends ApprovalExposureLimit> boolean exists(Example<S> example) { return false; }
        @Override public <S extends ApprovalExposureLimit, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<ApprovalExposureLimit> findAll(Sort sort) { return null; }
        @Override public Page<ApprovalExposureLimit> findAll(Pageable pageable) { return null; }
    }

    private static class MockBranchRepository implements BranchRepository {
        private final ConcurrentHashMap<Long, Branch> map = new ConcurrentHashMap<>();

        @Override public Optional<Branch> findById(Long id) { return Optional.ofNullable(map.get(id)); }
        @Override public Optional<Branch> findById(Long id, String bankName) { return findById(id); }
        @Override public Optional<Branch> findByCode(String code) { return Optional.empty(); }
        @Override public boolean existsByCode(String code) { return false; }
        @Override public Optional<Branch> findByCodeIgnoreCase(String code) { return Optional.empty(); }
        @Override public boolean existsByCodeIgnoreCase(String code) { return false; }
        @Override public boolean existsByCodeIgnoreCase(String code, String bankName) { return false; }
        @Override public boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id, String bankName) { return false; }
        @Override public List<Branch> findAll() { return new ArrayList<>(map.values()); }
        @Override public List<Branch> findAll(String bankName) { return findAll(); }
        @Override public Branch save(Branch branch) { map.put(branch.getId(), branch); return branch; }
    }
}
