package com.los.bank.customer.entity;

/**
 * Values of c001.onboarding_status.
 * DRAFT: the onboarding wizard is still being filled step by step.
 * COMPLETED: the final submit passed the completeness check.
 */
public final class CustomerOnboardingStatus {

    public static final String DRAFT = "DRAFT";
    public static final String COMPLETED = "COMPLETED";

    private CustomerOnboardingStatus() {
    }

    public static boolean isDraft(String status) {
        return DRAFT.equalsIgnoreCase(status);
    }
}
