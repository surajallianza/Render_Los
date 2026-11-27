package com.los.bank.branch.service;

import com.los.bank.branch.dto.BranchDto;
import com.los.bank.branch.dto.BranchRequestDto;
import com.los.bank.branch.entity.Branch;
import com.los.bank.branch.exception.BranchException;
import com.los.bank.branch.repository.BranchRepository;
import com.los.bank.setup.repository.BankInformationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BranchServiceImpl implements BranchService {

    private static final Logger logger = LoggerFactory.getLogger(BranchServiceImpl.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+ -]{7,15}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^[0-9a-zA-Z -]{3,10}$");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final BranchRepository branchRepository;
    private final BankInformationRepository bankInformationRepository;

    public BranchServiceImpl(
            BranchRepository branchRepository,
            BankInformationRepository bankInformationRepository) {
        this.branchRepository = branchRepository;
        this.bankInformationRepository = bankInformationRepository;
    }

    @Override
    public BranchDto createBranch(BranchRequestDto requestDto) {
        validateBranchRequest(requestDto);

        String branchCode = requestDto.getBranchCode().trim().toUpperCase();
        String bankName = requestDto.getBankName().trim();

        // Check duplicate branch code in the target dynamic bank database
        if (branchRepository.existsByCodeIgnoreCase(branchCode, bankName)) {
            throw new BranchException("Branch code '" + branchCode + "' already exists.", HttpStatus.CONFLICT);
        }

        // Verify referenced bank if existing banks exist in master
        verifyBankAssociation(bankName);

        LocalDate incDate = parseDate(requestDto.getIncorporationDate());
        LocalTime workFrom = parseTime(requestDto.getWorkingFrom(), "workingFrom");
        LocalTime workTo = parseTime(requestDto.getWorkingTo(), "workingTo");

        if (workFrom != null && workTo != null && !workTo.isAfter(workFrom)) {
            throw new BranchException("workingTo time must be later than workingFrom.", HttpStatus.BAD_REQUEST);
        }

        Branch branch = Branch.builder()
                .bankName(requestDto.getBankName().trim())
                .code(branchCode)
                .name(requestDto.getBranchName().trim())
                .rbiLicenseNo(requestDto.getRbiLicenseNo() != null ? requestDto.getRbiLicenseNo().trim() : null)
                .registrationNo(requestDto.getRegistrationNo() != null ? requestDto.getRegistrationNo().trim() : null)
                .incorporationDate(incDate)
                .workingFrom(workFrom)
                .workingTo(workTo)
                .address(requestDto.getAddress().trim())
                .address1(requestDto.getAddress1() != null ? requestDto.getAddress1().trim() : null)
                .address2(requestDto.getAddress2() != null ? requestDto.getAddress2().trim() : null)
                .city(requestDto.getCity().trim())
                .state(requestDto.getState().trim())
                .country(requestDto.getCountry().trim())
                .pincode(requestDto.getPinCode().trim())
                .primaryContactNumber(requestDto.getPrimaryContactNumber().trim())
                .secondaryContactNumber(requestDto.getSecondaryContactNumber() != null && !requestDto.getSecondaryContactNumber().trim().isEmpty()
                        ? requestDto.getSecondaryContactNumber().trim() : null)
                .emailId(requestDto.getEmailId().trim().toLowerCase())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Branch saved = branchRepository.save(branch);
        logger.info("Successfully created branch '{}' (code: '{}') with ID: {} in bank database", saved.getName(), saved.getCode(), saved.getId());
        return toDto(saved);
    }

    @Override
    public BranchDto updateBranch(Long id, BranchRequestDto requestDto) {
        if (id == null || id <= 0) {
            throw new BranchException("Invalid branch ID: " + id, HttpStatus.BAD_REQUEST);
        }

        validateBranchRequest(requestDto);

        String branchCode = requestDto.getBranchCode().trim().toUpperCase();
        String bankName = requestDto.getBankName().trim();

        // 1. Verify existing branch
        Branch existing = branchRepository.findById(id, bankName)
                .orElseThrow(() -> new BranchException("Branch not found with ID: " + id, HttpStatus.NOT_FOUND));

        // 2. Check duplicate code for another branch
        if (branchRepository.existsByCodeIgnoreCaseAndIdNot(branchCode, id, bankName)) {
            throw new BranchException("Branch code '" + branchCode + "' already exists.", HttpStatus.CONFLICT);
        }

        // 3. Verify referenced bank if existing banks exist in master
        verifyBankAssociation(bankName);

        LocalDate incDate = parseDate(requestDto.getIncorporationDate());
        LocalTime workFrom = parseTime(requestDto.getWorkingFrom(), "workingFrom");
        LocalTime workTo = parseTime(requestDto.getWorkingTo(), "workingTo");

        if (workFrom != null && workTo != null && !workTo.isAfter(workFrom)) {
            throw new BranchException("workingTo time must be later than workingFrom.", HttpStatus.BAD_REQUEST);
        }

        existing.setName(requestDto.getBranchName().trim());
        existing.setCode(branchCode);
        existing.setBankName(bankName);
        existing.setRbiLicenseNo(requestDto.getRbiLicenseNo() != null ? requestDto.getRbiLicenseNo().trim() : null);
        existing.setRegistrationNo(requestDto.getRegistrationNo() != null ? requestDto.getRegistrationNo().trim() : null);
        existing.setIncorporationDate(incDate);
        existing.setWorkingFrom(workFrom);
        existing.setWorkingTo(workTo);
        existing.setAddress(requestDto.getAddress().trim());
        existing.setAddress1(requestDto.getAddress1() != null ? requestDto.getAddress1().trim() : null);
        existing.setAddress2(requestDto.getAddress2() != null ? requestDto.getAddress2().trim() : null);
        existing.setCity(requestDto.getCity().trim());
        existing.setState(requestDto.getState().trim());
        existing.setCountry(requestDto.getCountry().trim());
        existing.setPincode(requestDto.getPinCode().trim());
        existing.setPrimaryContactNumber(requestDto.getPrimaryContactNumber().trim());
        existing.setSecondaryContactNumber(requestDto.getSecondaryContactNumber() != null && !requestDto.getSecondaryContactNumber().trim().isEmpty()
                ? requestDto.getSecondaryContactNumber().trim() : null);
        existing.setEmailId(requestDto.getEmailId().trim().toLowerCase());
        existing.setUpdatedAt(LocalDateTime.now());

        Branch updated = branchRepository.save(existing);
        logger.info("Successfully updated branch '{}' (code: '{}', ID: {})", updated.getName(), updated.getCode(), updated.getId());
        return toDto(updated);
    }

    @Override
    public List<BranchDto> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BranchDto getBranchById(Long id) {
        if (id == null || id <= 0) {
            throw new BranchException("Invalid branch ID: " + id, HttpStatus.BAD_REQUEST);
        }

        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new BranchException("Branch not found with ID: " + id, HttpStatus.NOT_FOUND));

        return toDto(branch);
    }

    private void validateBranchRequest(BranchRequestDto dto) {
        if (dto == null) {
            throw new BranchException("Request body cannot be null.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getBankName() == null || dto.getBankName().trim().isEmpty()) {
            throw new BranchException("Bank name is required.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getBranchCode() == null || dto.getBranchCode().trim().isEmpty()) {
            throw new BranchException("Branch code is required.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getBranchCode().trim().length() > 20) {
            throw new BranchException("Branch code cannot exceed 20 characters.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getBranchName() == null || dto.getBranchName().trim().isEmpty()) {
            throw new BranchException("Branch name is required.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getBranchName().trim().length() > 150) {
            throw new BranchException("Branch name cannot exceed 150 characters.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) {
            throw new BranchException("Address is required.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getCity() == null || dto.getCity().trim().isEmpty()) {
            throw new BranchException("City is required.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getState() == null || dto.getState().trim().isEmpty()) {
            throw new BranchException("State is required.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getCountry() == null || dto.getCountry().trim().isEmpty()) {
            throw new BranchException("Country is required.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getPinCode() == null || dto.getPinCode().trim().isEmpty()) {
            throw new BranchException("Pin code is required.", HttpStatus.BAD_REQUEST);
        }
        if (!PIN_PATTERN.matcher(dto.getPinCode().trim()).matches()) {
            throw new BranchException("Invalid pin code format: '" + dto.getPinCode() + "'.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getPrimaryContactNumber() == null || dto.getPrimaryContactNumber().trim().isEmpty()) {
            throw new BranchException("Primary contact number is required.", HttpStatus.BAD_REQUEST);
        }
        if (!PHONE_PATTERN.matcher(dto.getPrimaryContactNumber().trim()).matches()) {
            throw new BranchException("Invalid primary contact number format: '" + dto.getPrimaryContactNumber() + "'.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getSecondaryContactNumber() != null && !dto.getSecondaryContactNumber().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(dto.getSecondaryContactNumber().trim()).matches()) {
                throw new BranchException("Invalid secondary contact number format: '" + dto.getSecondaryContactNumber() + "'.", HttpStatus.BAD_REQUEST);
            }
        }

        if (dto.getEmailId() == null || dto.getEmailId().trim().isEmpty()) {
            throw new BranchException("Email address is required.", HttpStatus.BAD_REQUEST);
        }
        if (!EMAIL_PATTERN.matcher(dto.getEmailId().trim()).matches()) {
            throw new BranchException("Invalid email address format: '" + dto.getEmailId() + "'.", HttpStatus.BAD_REQUEST);
        }
    }

    private void verifyBankAssociation(String bankName) {
        if (bankName == null || bankName.trim().isEmpty()) {
            return;
        }
        long count = bankInformationRepository.count();
        if (count > 0) {
            String trimmed = bankName.trim();
            boolean exists = bankInformationRepository.findByBankNameIgnoreCase(trimmed).isPresent()
                    || bankInformationRepository.findByBankCode(trimmed.toUpperCase()).isPresent();
            if (!exists) {
                throw new BranchException("Referenced bank does not exist: '" + trimmed + "'.", HttpStatus.NOT_FOUND);
            }
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new BranchException("Invalid incorporation date format: '" + dateStr + "'. Expected format: yyyy-MM-dd.", HttpStatus.BAD_REQUEST);
        }
    }

    private LocalTime parseTime(String timeStr, String fieldName) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            String trimmed = timeStr.trim();
            if (trimmed.length() == 5) {
                return LocalTime.parse(trimmed, TIME_FORMATTER);
            }
            return LocalTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (Exception e) {
            throw new BranchException("Invalid " + fieldName + " time format: '" + timeStr + "'. Expected format: HH:mm.", HttpStatus.BAD_REQUEST);
        }
    }

    private BranchDto toDto(Branch entity) {
        if (entity == null) {
            return null;
        }
        return BranchDto.builder()
                .id(entity.getId())
                .bankName(entity.getBankName())
                .branchCode(entity.getCode())
                .branchName(entity.getName())
                .rbiLicenseNo(entity.getRbiLicenseNo())
                .registrationNo(entity.getRegistrationNo())
                .incorporationDate(entity.getIncorporationDate() != null ? entity.getIncorporationDate().toString() : null)
                .workingFrom(entity.getWorkingFrom() != null ? entity.getWorkingFrom().format(TIME_FORMATTER) : null)
                .workingTo(entity.getWorkingTo() != null ? entity.getWorkingTo().format(TIME_FORMATTER) : null)
                .address(entity.getAddress())
                .address1(entity.getAddress1())
                .address2(entity.getAddress2())
                .city(entity.getCity())
                .state(entity.getState())
                .country(entity.getCountry())
                .pinCode(entity.getPincode())
                .primaryContactNumber(entity.getPrimaryContactNumber())
                .secondaryContactNumber(entity.getSecondaryContactNumber())
                .emailId(entity.getEmailId())
                .build();
    }
}
