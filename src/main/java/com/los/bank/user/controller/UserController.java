package com.los.bank.user.controller;

import com.los.bank.user.context.BankContext;
import com.los.bank.user.datasource.DynamicBankDataSourceManager;
import com.los.bank.user.dto.ApiResponse;
import com.los.bank.user.dto.UserCreateRequest;
import com.los.bank.user.dto.UserResponse;
import com.los.bank.user.dto.UserUpdateRequest;
import com.los.bank.user.entity.UserToken;
import com.los.bank.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final DynamicBankDataSourceManager dataSourceManager;

    public UserController(UserService userService, DynamicBankDataSourceManager dataSourceManager) {
        this.userService = userService;
        this.dataSourceManager = dataSourceManager;
    }

    /**
     * API 1: GET – Fetch all users
     * Fetches all registered users from the targeted bank database without exposing sensitive credentials.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(value = "bankCode", required = false) String bankCode) {
        ensureBankContextIfPresent(bankCode);
        logger.info("Received request to fetch all users (BankContext: '{}')", BankContext.getCurrentDatabase());
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    /**
     * API 2: POST – Create user with password
     * Enforces password confirmation, BCrypt hashing, prevents plaintext storage,
     * seeds profile, security settings, branch mapping, and saves an authentication token in u007.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@jakarta.validation.Valid @RequestBody UserCreateRequest request) {
        if (request != null && request.getBankCode() != null && !request.getBankCode().trim().isEmpty()) {
            ensureBankContextIfPresent(request.getBankCode());
        }
        logger.info("Received request to create user: username='{}', empNo='{}' (Target DB: '{}')",
                request != null ? request.getUsername() : "null",
                request != null ? request.getEmpNo() : "null",
                BankContext.getCurrentDatabase());

        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", response));
    }

    /**
     * API 3: PUT – Update user and optional password
     * Updates profile, security, and credentials in the target bank database.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable("id") Long id,
            @jakarta.validation.Valid @RequestBody UserUpdateRequest request) {
        if (request != null && request.getBankCode() != null && !request.getBankCode().trim().isEmpty()) {
            ensureBankContextIfPresent(request.getBankCode());
        }
        logger.info("Received request to update user: id={} (Target DB: '{}')", id, BankContext.getCurrentDatabase());
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    /**
     * API 4: GET – Fetch user by ID
     * Retrieves specific user details by primary ID from the target bank database.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable("id") Long id,
            @RequestParam(value = "bankCode", required = false) String bankCode) {
        ensureBankContextIfPresent(bankCode);
        logger.info("Received request to fetch user: id={} (Target DB: '{}')", id, BankContext.getCurrentDatabase());
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }

    /**
     * Additional Token Management endpoint to save or refresh an authentication token.
     */
    @PostMapping("/{id}/token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveUserToken(
            @PathVariable("id") Long id,
            @RequestParam(value = "expirationHours", defaultValue = "24") int expirationHours,
            @RequestParam(value = "bankCode", required = false) String bankCode,
            @RequestBody(required = false) Map<String, String> tokenPayload) {
        ensureBankContextIfPresent(bankCode);
        String tokenValue = tokenPayload != null ? tokenPayload.get("token") : null;
        UserToken userToken = userService.saveToken(id, tokenValue, expirationHours);
        Map<String, Object> tokenData = Map.of(
                "token_id", userToken.getId(),
                "user_id", userToken.getUserId(),
                "expires_at", userToken.getExpiresAt(),
                "created_at", userToken.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Token saved successfully", tokenData));
    }

    private void ensureBankContextIfPresent(String bankIdentifier) {
        if (bankIdentifier != null && !bankIdentifier.trim().isEmpty()) {
            String db = dataSourceManager.resolveBankDatabase(bankIdentifier);
            BankContext.setCurrentDatabase(db);
        } else if (BankContext.getCurrentDatabase() == null) {
            String defaultDb = dataSourceManager.resolveDefaultBankDatabase();
            BankContext.setCurrentDatabase(defaultDb);
        }
    }
}
