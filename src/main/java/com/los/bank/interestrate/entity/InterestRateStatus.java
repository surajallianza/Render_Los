package com.los.bank.interestrate.entity;

/**
 * Values of i001.status (maker-checker lifecycle of a rate sheet).
 */
public final class InterestRateStatus {

    /** Created or edited by the maker, waiting for a checker. */
    public static final String PENDING = "PENDING";
    /** Approved by a checker; used for loans from its effective date. */
    public static final String APPROVED = "APPROVED";
    /** Rejected by a checker; can be edited and sent again. */
    public static final String REJECTED = "REJECTED";

    private InterestRateStatus() {
    }
}
