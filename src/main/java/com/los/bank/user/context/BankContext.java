package com.los.bank.user.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThreadLocal context holder for the target bank database.
 * Ensures the bank database key is established before any JPA transaction begins.
 */
public final class BankContext {

    private static final Logger logger = LoggerFactory.getLogger(BankContext.class);

    private static final ThreadLocal<String> CURRENT_DATABASE = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_BANK_CODE = new ThreadLocal<>();

    private BankContext() {
    }

    public static void setCurrentDatabase(String databaseName) {
        if (databaseName != null && !databaseName.trim().isEmpty()) {
            String db = databaseName.trim().toLowerCase();
            logger.debug("[BankContext] Setting current target database to '{}'", db);
            CURRENT_DATABASE.set(db);
        } else {
            CURRENT_DATABASE.remove();
        }
    }

    public static String getCurrentDatabase() {
        return CURRENT_DATABASE.get();
    }

    public static void setCurrentBankCode(String bankCode) {
        if (bankCode != null && !bankCode.trim().isEmpty()) {
            CURRENT_BANK_CODE.set(bankCode.trim().toUpperCase());
        } else {
            CURRENT_BANK_CODE.remove();
        }
    }

    public static String getCurrentBankCode() {
        return CURRENT_BANK_CODE.get();
    }

    public static void clear() {
        logger.debug("[BankContext] Clearing context (was db='{}', code='{}')",
                CURRENT_DATABASE.get(), CURRENT_BANK_CODE.get());
        CURRENT_DATABASE.remove();
        CURRENT_BANK_CODE.remove();
    }
}
