package com.los.bank.bankmaster;

import com.los.bank.bankmaster.dto.BankAccountRequestDto;
import com.los.bank.bankmaster.dto.BankAccountResponseDto;
import com.los.bank.bankmaster.entity.BankAccount;
import com.los.bank.bankmaster.exception.BankMasterException;
import com.los.bank.bankmaster.repository.BankAccountRepository;
import com.los.bank.bankmaster.service.BankAccountServiceImpl;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountServiceTest {

    private BankAccountServiceImpl service;
    private MockBankAccountRepository accountRepository;
    private MockBranchRepository branchRepository;

    @BeforeEach
    void setUp() {
        accountRepository = new MockBankAccountRepository();
        branchRepository = new MockBranchRepository();
        service = new BankAccountServiceImpl(accountRepository, branchRepository);

        Branch b = Branch.builder().id(101L).name("Pune Main Branch").code("PUN001").build();
        branchRepository.save(b);
    }

    @Test
    @DisplayName("Should successfully create a bank account")
    void testCreateBankAccount() {
        BankAccountRequestDto req = new BankAccountRequestDto();
        req.setBankName("State Bank of India");
        req.setBranchId(101L);
        req.setParticulars("Collection Account");
        req.setAccountType("Current");
        req.setAccountNumber("123456789012");
        req.setMicrCode("400002123");
        req.setIfscCode("SBIN0001234");

        BankAccountResponseDto res = service.createBankAccount(req);

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals("State Bank of India", res.getBankName());
        assertEquals("Pune Main Branch", res.getBranchName());
        assertEquals("123456789012", res.getAccountNumber());
        assertEquals("SBIN0001234", res.getIfscCode());
        assertTrue(res.getIsActive());
    }

    @Test
    @DisplayName("Should reject duplicate active account number for same branch")
    void testRejectDuplicateAccount() {
        BankAccountRequestDto req = new BankAccountRequestDto();
        req.setBankName("State Bank of India");
        req.setBranchId(101L);
        req.setAccountType("Current");
        req.setAccountNumber("123456789012");
        req.setIfscCode("SBIN0001234");

        service.createBankAccount(req);

        BankMasterException ex = assertThrows(BankMasterException.class, () -> service.createBankAccount(req));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Should validate IFSC format")
    void testInvalidIfscCode() {
        BankAccountRequestDto req = new BankAccountRequestDto();
        req.setBankName("SBI");
        req.setAccountType("Current");
        req.setAccountNumber("123456789012");
        req.setIfscCode("INVALID_IFSC");

        BankMasterException ex = assertThrows(BankMasterException.class, () -> service.createBankAccount(req));
        assertTrue(ex.getMessage().contains("valid 11-character IFSC code"));
    }

    @Test
    @DisplayName("Should retrieve account by ID and update")
    void testGetAndUpdateAccount() {
        BankAccountRequestDto req = new BankAccountRequestDto();
        req.setBankName("SBI");
        req.setBranchId(101L);
        req.setAccountType("Current");
        req.setAccountNumber("987654321012");
        req.setIfscCode("SBIN0005678");

        BankAccountResponseDto created = service.createBankAccount(req);
        Long id = created.getId();

        BankAccountResponseDto fetched = service.getBankAccountById(id);
        assertEquals("987654321012", fetched.getAccountNumber());

        // Update
        req.setParticulars("Updated Disbursement Account");
        BankAccountResponseDto updated = service.updateBankAccount(id, req);
        assertEquals("Updated Disbursement Account", updated.getParticulars());
    }

    @Test
    @DisplayName("Should soft-delete account")
    void testSoftDeleteAccount() {
        BankAccountRequestDto req = new BankAccountRequestDto();
        req.setBankName("SBI");
        req.setBranchId(101L);
        req.setAccountType("Savings");
        req.setAccountNumber("555566667777");
        req.setIfscCode("SBIN0009999");

        BankAccountResponseDto created = service.createBankAccount(req);
        Long id = created.getId();

        service.deleteBankAccount(id);

        assertThrows(BankMasterException.class, () -> service.getBankAccountById(id));
    }

    // =========================================================================
    // IN-MEMORY MOCKS
    // =========================================================================
    private static class MockBankAccountRepository implements BankAccountRepository {
        private final ConcurrentHashMap<Long, BankAccount> store = new ConcurrentHashMap<>();
        private final AtomicLong seq = new AtomicLong(1);

        @Override
        public BankAccount save(BankAccount entity) {
            if (entity.getId() == null) {
                entity.setId(seq.getAndIncrement());
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public List<BankAccount> findByIsActiveTrueOrderByIdDesc() {
            List<BankAccount> list = new ArrayList<>();
            for (BankAccount b : store.values()) {
                if (Boolean.TRUE.equals(b.getIsActive())) list.add(b);
            }
            return list;
        }

        @Override
        public Optional<BankAccount> findByIdAndIsActiveTrue(Long id) {
            BankAccount b = store.get(id);
            if (b != null && Boolean.TRUE.equals(b.getIsActive())) {
                return Optional.of(b);
            }
            return Optional.empty();
        }

        @Override
        public List<BankAccount> findByBranchIdAndIsActiveTrue(Long branchId) {
            List<BankAccount> list = new ArrayList<>();
            for (BankAccount b : store.values()) {
                if (Boolean.TRUE.equals(b.getIsActive()) && branchId != null && branchId.equals(b.getBranchId())) {
                    list.add(b);
                }
            }
            return list;
        }

        @Override
        public boolean existsActiveAccount(String accountNumber, String ifscCode, Long branchId) {
            return store.values().stream().anyMatch(b ->
                    Boolean.TRUE.equals(b.getIsActive())
                    && accountNumber.equals(b.getAccountNumber())
                    && ifscCode.equalsIgnoreCase(b.getIfscCode())
                    && (branchId == null && b.getBranchId() == null || branchId != null && branchId.equals(b.getBranchId())));
        }

        @Override
        public boolean existsActiveAccountExcludingId(String accountNumber, String ifscCode, Long branchId, Long id) {
            return store.values().stream().anyMatch(b ->
                    Boolean.TRUE.equals(b.getIsActive())
                    && !b.getId().equals(id)
                    && accountNumber.equals(b.getAccountNumber())
                    && ifscCode.equalsIgnoreCase(b.getIfscCode())
                    && (branchId == null && b.getBranchId() == null || branchId != null && branchId.equals(b.getBranchId())));
        }

        @Override public Optional<BankAccount> findById(Long aLong) { return Optional.ofNullable(store.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return store.containsKey(aLong); }
        @Override public <S extends BankAccount> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public List<BankAccount> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<BankAccount> findAllById(Iterable<Long> longs) { return null; }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long aLong) { store.remove(aLong); }
        @Override public void delete(BankAccount entity) { store.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends BankAccount> entities) {}
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override public <S extends BankAccount> S saveAndFlush(S entity) { return null; }
        @Override public <S extends BankAccount> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<BankAccount> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public BankAccount getOne(Long aLong) { return null; }
        @Override public BankAccount getById(Long aLong) { return null; }
        @Override public BankAccount getReferenceById(Long aLong) { return null; }
        @Override public <S extends BankAccount> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends BankAccount> List<S> findAll(Example<S> example) { return null; }
        @Override public <S extends BankAccount> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override public <S extends BankAccount> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends BankAccount> long count(Example<S> example) { return 0; }
        @Override public <S extends BankAccount> boolean exists(Example<S> example) { return false; }
        @Override public <S extends BankAccount, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<BankAccount> findAll(Sort sort) { return null; }
        @Override public Page<BankAccount> findAll(Pageable pageable) { return null; }
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
