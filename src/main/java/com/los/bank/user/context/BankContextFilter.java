package com.los.bank.user.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.los.bank.user.datasource.DynamicBankDataSourceManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that establishes the BankContext BEFORE any Controller or JPA transaction begins.
 * Inspects headers, query parameters, and JSON request bodies for bank identifiers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class BankContextFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(BankContextFilter.class);

    private final DynamicBankDataSourceManager dataSourceManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BankContextFilter(DynamicBankDataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Master setup module uses setupDataSource (bank_setup); all other LOS endpoints route to bank tenant DB
        return path.startsWith("/api/setup") || path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        HttpServletRequest requestToUse = request;
        String contentType = request.getContentType();
        boolean isJson = contentType != null && contentType.toLowerCase().contains("application/json");

        if (isJson) {
            requestToUse = new CachedBodyHttpServletRequest(request);
        }

        String bankIdentifier = extractBankIdentifier(requestToUse);
        String resolvedDatabase = dataSourceManager.resolveBankDatabase(bankIdentifier);

        logger.info("[BankContextFilter] Request: {} {}, Extracted Bank Identifier: '{}' -> Resolved Database: '{}'",
                request.getMethod(), request.getRequestURI(), bankIdentifier, resolvedDatabase);

        try {
            BankContext.setCurrentDatabase(resolvedDatabase);
            if (bankIdentifier != null) {
                BankContext.setCurrentBankCode(bankIdentifier);
            }
            filterChain.doFilter(requestToUse, response);
        } finally {
            BankContext.clear();
        }
    }

    private String extractBankIdentifier(HttpServletRequest request) {
        // 1. Check HTTP Headers
        String header = request.getHeader("X-Bank-Code");
        if (header != null && !header.trim().isEmpty()) {
            return header.trim();
        }
        header = request.getHeader("x-bank-code");
        if (header != null && !header.trim().isEmpty()) {
            return header.trim();
        }
        header = request.getHeader("Bank-Code");
        if (header != null && !header.trim().isEmpty()) {
            return header.trim();
        }
        header = request.getHeader("X-Database-Name");
        if (header != null && !header.trim().isEmpty()) {
            return header.trim();
        }
        header = request.getHeader("x-database-name");
        if (header != null && !header.trim().isEmpty()) {
            return header.trim();
        }

        // 2. Check Query Parameters
        String param = request.getParameter("bankCode");
        if (param != null && !param.trim().isEmpty()) {
            return param.trim();
        }
        param = request.getParameter("bank_code");
        if (param != null && !param.trim().isEmpty()) {
            return param.trim();
        }
        param = request.getParameter("databaseName");
        if (param != null && !param.trim().isEmpty()) {
            return param.trim();
        }
        param = request.getParameter("database_name");
        if (param != null && !param.trim().isEmpty()) {
            return param.trim();
        }

        // 3. Inspect JSON Body for POST / PUT requests
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            if (request instanceof CachedBodyHttpServletRequest cachedRequest) {
                try {
                    String body = cachedRequest.getBodyAsString();
                    if (body != null && !body.trim().isEmpty()) {
                        JsonNode root = objectMapper.readTree(body);
                        if (root.has("bank_code") && !root.get("bank_code").asText().isEmpty()) {
                            return root.get("bank_code").asText().trim();
                        }
                        if (root.has("bankCode") && !root.get("bankCode").asText().isEmpty()) {
                            return root.get("bankCode").asText().trim();
                        }
                        if (root.has("database_name") && !root.get("database_name").asText().isEmpty()) {
                            return root.get("database_name").asText().trim();
                        }
                        if (root.has("databaseName") && !root.get("databaseName").asText().isEmpty()) {
                            return root.get("databaseName").asText().trim();
                        }
                    }
                } catch (Exception e) {
                    logger.debug("[BankContextFilter] Failed to parse JSON body for bank identifier: {}", e.getMessage());
                }
            }
        }

        return null;
    }
}
