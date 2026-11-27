CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS b001 (
    id                              BIGSERIAL PRIMARY KEY,
    name                            VARCHAR(150) NOT NULL,
    code                            VARCHAR(20) NOT NULL,
    bank_name                       VARCHAR(150),
    rbi_license_no                  VARCHAR(50),
    registration_no                 VARCHAR(50),
    incorporation_date              DATE,
    working_from                    TIME,
    working_to                      TIME,
    address                         VARCHAR(255),
    address1                        VARCHAR(255),
    address2                        VARCHAR(255),
    country                         VARCHAR(100) DEFAULT 'India',
    city                            VARCHAR(100),
    state                           VARCHAR(100),
    pincode                         VARCHAR(10),
    primary_contact_number          VARCHAR(20),
    secondary_contact_number        VARCHAR(20),
    email_id                        VARCHAR(150),
    status                          VARCHAR(20),
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP,
    CONSTRAINT uq_b001_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS r001 (
    id                              SERIAL PRIMARY KEY,
    name                            VARCHAR(50) NOT NULL,
    panel                           VARCHAR(30),
    description                     VARCHAR(255),
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS r002 (
    id                              SERIAL PRIMARY KEY,
    code                            VARCHAR(100) NOT NULL,
    description                     VARCHAR(255),
    module                          VARCHAR(60),
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_r002_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS r003 (
    role_id                         INT NOT NULL,
    permission_id                   INT NOT NULL,
    CONSTRAINT pk_r003 PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_r003_role_id FOREIGN KEY (role_id) REFERENCES r001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_r003_permission_id FOREIGN KEY (permission_id) REFERENCES r002 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS u001 (
    id                              BIGSERIAL PRIMARY KEY,
    emp_no                          VARCHAR(30) NOT NULL,
    username                        VARCHAR(80) NOT NULL,
    password_hash                   VARCHAR(255) NOT NULL,
    status                          VARCHAR(30),
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    no_of_bad_logins                INT NOT NULL DEFAULT 0,
    last_login_date                 DATE,
    last_login_time                 TIME,
    CONSTRAINT uq_u001_emp_no UNIQUE (emp_no),
    CONSTRAINT uq_u001_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS r004 (
    id                              BIGSERIAL PRIMARY KEY,
    designation                     VARCHAR(100) NOT NULL,
    role_id                         INT,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    created_by                      BIGINT,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by                     BIGINT,
    updated_at                      TIMESTAMP,
    CONSTRAINT fk_r004_role_id FOREIGN KEY (role_id) REFERENCES r001 (id),
    CONSTRAINT fk_r004_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_r004_modified_by FOREIGN KEY (modified_by) REFERENCES u001 (id)
);

CREATE TABLE IF NOT EXISTS r005 (
    id                              BIGSERIAL PRIMARY KEY,
    target_type                     VARCHAR(30),
    target_name                     VARCHAR(100),
    permission_code                 VARCHAR(100) NOT NULL,
    effect                          VARCHAR(10),
    reason                          VARCHAR(255),
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    created_by                      BIGINT,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by                     BIGINT,
    updated_at                      TIMESTAMP,
    CONSTRAINT fk_r005_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_r005_modified_by FOREIGN KEY (modified_by) REFERENCES u001 (id),
    CONSTRAINT fk_r005_permission_code FOREIGN KEY (permission_code) REFERENCES r002 (code)
);

CREATE TABLE IF NOT EXISTS u002 (
    user_id                         BIGINT NOT NULL,
    first_name                      VARCHAR(80) NOT NULL,
    middle_name                     VARCHAR(80),
    last_name                       VARCHAR(80),
    dob                             DATE,
    gender                          VARCHAR(10),
    email                           VARCHAR(150),
    mobile                          VARCHAR(20),
    designation                     VARCHAR(100),
    role_id                         INT,
    CONSTRAINT pk_u002 PRIMARY KEY (user_id),
    CONSTRAINT uq_u002_email UNIQUE (email),
    CONSTRAINT fk_u002_user_id FOREIGN KEY (user_id) REFERENCES u001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_u002_role_id FOREIGN KEY (role_id) REFERENCES r001 (id)
);

CREATE TABLE IF NOT EXISTS u003 (
    user_id                         BIGINT NOT NULL,
    system_role                     INT NOT NULL,
    login_branch_id                 BIGINT,
    login_time                      TIME,
    logout_time                     TIME,
    inactive_session_timeout        INT,
    created_by                      BIGINT,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_by                     BIGINT,
    verified_date                   TIMESTAMP,
    modified_by                     BIGINT,
    updated_at                      TIMESTAMP,
    CONSTRAINT pk_u003 PRIMARY KEY (user_id),
    CONSTRAINT fk_u003_user_id FOREIGN KEY (user_id) REFERENCES u001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_u003_login_branch_id FOREIGN KEY (login_branch_id) REFERENCES b001 (id),
    CONSTRAINT fk_u003_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_u003_verified_by FOREIGN KEY (verified_by) REFERENCES u001 (id),
    CONSTRAINT fk_u003_modified_by FOREIGN KEY (modified_by) REFERENCES u001 (id)
);

CREATE TABLE IF NOT EXISTS u004 (
    title                           VARCHAR(20) NOT NULL, 
    user_id                         BIGINT NOT NULL,
    branch_id                       BIGINT NOT NULL,
    CONSTRAINT pk_u004 PRIMARY KEY (user_id, branch_id),
    CONSTRAINT fk_u004_user_id FOREIGN KEY (user_id) REFERENCES u001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_u004_branch_id FOREIGN KEY (branch_id) REFERENCES b001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS u005 (
    id                              BIGSERIAL PRIMARY KEY,
    user_id                         BIGINT NOT NULL,
    otp_hash                        VARCHAR(255) NOT NULL,
    used                            BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at                      TIMESTAMP NOT NULL,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_u005_user_id FOREIGN KEY (user_id) REFERENCES u001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS u006 (
    id                              BIGSERIAL PRIMARY KEY,
    user_id                         BIGINT NOT NULL,
    new_password_hash               VARCHAR(255) NOT NULL,
    created_by                      BIGINT,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_by                     BIGINT,
    verified_date                   TIMESTAMP,
    applied                         BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at                      TIMESTAMP,
    CONSTRAINT fk_u006_user_id FOREIGN KEY (user_id) REFERENCES u001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_u006_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_u006_verified_by FOREIGN KEY (verified_by) REFERENCES u001 (id)
);

CREATE TABLE IF NOT EXISTS u007 (
    id                              BIGSERIAL PRIMARY KEY,
    user_id                         BIGINT NOT NULL,
    token_hash                      VARCHAR(255) NOT NULL,
    used                            BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at                      TIMESTAMP NOT NULL,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_u007_user_id FOREIGN KEY (user_id) REFERENCES u001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS u008 (
    jti                             VARCHAR(100) NOT NULL,
    user_id                         BIGINT NOT NULL,
    last_seen                       TIMESTAMP,
    timeout_secs                    INT,
    invalidated                     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_u008 PRIMARY KEY (jti),
    CONSTRAINT fk_u008_user_id FOREIGN KEY (user_id) REFERENCES u001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS u009 (
    id                              BIGSERIAL PRIMARY KEY,
    staff_user_id                   BIGINT NOT NULL,
    token_hash                      VARCHAR(500) NOT NULL,
    organization_code               VARCHAR(30),
    expiry_date                     TIMESTAMP NOT NULL,
    revoked                         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_u009_staff_user_id FOREIGN KEY (staff_user_id) REFERENCES u001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS c001 (
    id                              BIGSERIAL PRIMARY KEY,
    customer_code                   VARCHAR(30) NOT NULL,
    branch_id                       BIGINT,
    password_hash                   VARCHAR(255) NOT NULL,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at                   TIMESTAMP,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP,
    onboarding_status               VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at                    TIMESTAMP,
    submitted_by                    BIGINT,
    CONSTRAINT uq_c001_customer_code UNIQUE (customer_code),
    CONSTRAINT fk_c001_branch_id FOREIGN KEY (branch_id) REFERENCES b001 (id),
    CONSTRAINT fk_c001_submitted_by FOREIGN KEY (submitted_by) REFERENCES u001 (id),
    CONSTRAINT ck_c001_onboarding_status CHECK (onboarding_status IN ('DRAFT', 'COMPLETED'))
);
COMMENT ON COLUMN c001.onboarding_status IS 'Onboarding wizard state: DRAFT while steps are being filled, COMPLETED after final submit. is_active stays the "customer account enabled" flag.';
COMMENT ON COLUMN c001.submitted_at IS 'When the onboarding form was submitted (DRAFT -> COMPLETED).';
COMMENT ON COLUMN c001.submitted_by IS 'Staff user (u001.id) who submitted the onboarding form.';

CREATE TABLE IF NOT EXISTS c002 (
    customer_id                     BIGINT NOT NULL,
    first_name                      VARCHAR(80) NOT NULL,
    middle_name                     VARCHAR(80),
    last_name                       VARCHAR(80),
    email                           VARCHAR(150),
    phone                           VARCHAR(20),
    CONSTRAINT pk_c002 PRIMARY KEY (customer_id),
    CONSTRAINT fk_c002_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS c003 (
    id                              BIGSERIAL PRIMARY KEY,
    customer_id                     BIGINT NOT NULL,
    token_hash                      VARCHAR(500) NOT NULL,
    expiry_date                     TIMESTAMP NOT NULL,
    revoked                         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_c003_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS m001 (
    id                              BIGSERIAL PRIMARY KEY,
    code                            VARCHAR(50) NOT NULL,
    description                     VARCHAR(255),
    is_fixed                        BOOLEAN NOT NULL DEFAULT FALSE,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    created_by                      BIGINT,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by                     BIGINT,
    updated_at                      TIMESTAMP,
    CONSTRAINT uq_m001_code UNIQUE (code),
    CONSTRAINT fk_m001_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_m001_modified_by FOREIGN KEY (modified_by) REFERENCES u001 (id)
);

CREATE TABLE IF NOT EXISTS m002 (
    id                              BIGSERIAL PRIMARY KEY,
    lookup_type_code                VARCHAR(50) NOT NULL,
    sub_type_code                   VARCHAR(50) NOT NULL,
    sub_type_description            VARCHAR(255),
    is_fixed                        BOOLEAN NOT NULL DEFAULT FALSE,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    display_order                   INT,
    created_by                      BIGINT,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by                     BIGINT,
    updated_at                      TIMESTAMP,
    CONSTRAINT fk_m002_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_m002_modified_by FOREIGN KEY (modified_by) REFERENCES u001 (id),
    CONSTRAINT fk_m002_lookup_type_code FOREIGN KEY (lookup_type_code) REFERENCES m001 (code)
);

CREATE TABLE IF NOT EXISTS m003 (
    id                              BIGSERIAL PRIMARY KEY,
    lookup_type_code                VARCHAR(50) NOT NULL,
    permission_code                 VARCHAR(100) NOT NULL,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_m003_lookup_type_code FOREIGN KEY (lookup_type_code) REFERENCES m001 (code),
    CONSTRAINT fk_m003_permission_code FOREIGN KEY (permission_code) REFERENCES r002 (code)
);

CREATE TABLE IF NOT EXISTS l001 (
    lead_id                         VARCHAR(50) NOT NULL,
    customer_id                     BIGINT,
    lead_status                     VARCHAR(50),
    assigned_user_id                BIGINT,
    assignment_timestamp            TIMESTAMP,
    assigned_by                     BIGINT,
    lead_acquisition_channel        VARCHAR(100),
    date                            DATE,
    sourcing_agent_partner_id       VARCHAR(100),

    employee_name                   VARCHAR(150),
    agent_partner_name              VARCHAR(150),
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP,
    branch	                        VARCHAR(50),
    CONSTRAINT pk_l001 PRIMARY KEY (lead_id),
    CONSTRAINT fk_l001_assigned_user_id FOREIGN KEY (assigned_user_id) REFERENCES u001 (id),
    CONSTRAINT fk_l001_assigned_by FOREIGN KEY (assigned_by) REFERENCES u001 (id),
    CONSTRAINT fk_l001_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id)
);

CREATE TABLE IF NOT EXISTS l002 (
    lead_id                         VARCHAR(50) NOT NULL,
    first_name_business_name        VARCHAR(150),
    name_prefix                     VARCHAR(20),
    middle_name                     VARCHAR(80),
    last_name                       VARCHAR(100),
    dob                             DATE,
    customer_type                   VARCHAR(50),
    age                             INT,
    gender                          VARCHAR(20),
    marital_status                  VARCHAR(50),
    residential_status              VARCHAR(50),
    email_address                   VARCHAR(150),
    mobile_number                   VARCHAR(50),
    pin_code                        VARCHAR(20),
    number_of_dependents            INT,
    customer_name                   VARCHAR(250),
    
    CONSTRAINT pk_l002 PRIMARY KEY (lead_id),
    CONSTRAINT fk_l002_lead_id FOREIGN KEY (lead_id) REFERENCES l001 (lead_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS l003 (
    lead_id                         VARCHAR(50) NOT NULL,
    pan_number                      BYTEA,
    pan_validation_status           VARCHAR(50),
    aadhaar_number                  BYTEA,
    aadhaar_validation_status       VARCHAR(50),
    passport_number                 BYTEA,
    passport_expiry_date            DATE,
    dedupe_status                   VARCHAR(50),
    blacklist_status                VARCHAR(50),
    CONSTRAINT pk_l003 PRIMARY KEY (lead_id),
    CONSTRAINT fk_l003_lead_id FOREIGN KEY (lead_id) REFERENCES l001 (lead_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS l004 (
    lead_id                         VARCHAR(50) NOT NULL,
    loan_product_type               VARCHAR(100),
    loan_amount                     NUMERIC(15,2),
    purpose_of_loan                 VARCHAR(255),
    tenure                          INT,
    number_of_instalments           INT,
    emi                             NUMERIC(15,2),
    interest_rate                   NUMERIC(8,4),
    total_interest                  NUMERIC(15,2),
    security_amount                 NUMERIC(15,2),
    down_payment_collateral         VARCHAR(255),
    other_loan_purpose           	VARCHAR(255),
    tenure_unit	                    VARCHAR(20),
    CONSTRAINT pk_l004 PRIMARY KEY (lead_id),
    CONSTRAINT fk_l004_lead_id FOREIGN KEY (lead_id) REFERENCES l001 (lead_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS l005 (
    lead_id                         VARCHAR(50) NOT NULL,
    employment_type                 VARCHAR(50),
    annual_income                   NUMERIC(15,2),
    designation                     VARCHAR(100),
    employer_business_name          VARCHAR(200),
    location                        VARCHAR(100),
   -- state                           VARCHAR(100),
    take_home_pay                   NUMERIC(15,2),
    deductions_or_emis_payable      NUMERIC(15,2),
    bank_name                       VARCHAR(150),
    primary_bank_account            BYTEA,
    account_statement_consent       VARCHAR(50),
    cibil_liability_check           VARCHAR(50),
    debt_to_income_ratio            NUMERIC(8,4),
    loan_to_value_ratio             NUMERIC(8,4),
    debt_service_coverage_ratio     NUMERIC(8,4),
    net_disposable_income           NUMERIC(15,2),
    occupation	                    VARCHAR(100),
    employer_type	                VARCHAR(100),
    years_of_service                VARCHAR(50),
    income_source                	VARCHAR(100),
    emp_email_id	                VARCHAR(150),
    emi_bounce                      VARCHAR(20) DEFAULT 'No',
    emi_bounce_count                INT DEFAULT 0,
    CONSTRAINT pk_l005 PRIMARY KEY (lead_id),
    CONSTRAINT fk_l005_lead_id FOREIGN KEY (lead_id) REFERENCES l001 (lead_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- l006 : Lead Address Details   [Leads]
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS l006 (
    lead_id                         VARCHAR(50) NOT NULL,
    particulars                     VARCHAR(100),
    house_flat_no                   VARCHAR(100),
    street_address                  VARCHAR(255),
    locality                        VARCHAR(150),
    city                            VARCHAR(100),
    state                           VARCHAR(100),
    pincode                         VARCHAR(10),
    CONSTRAINT pk_l006 PRIMARY KEY (lead_id),
    CONSTRAINT fk_l006_lead_id FOREIGN KEY (lead_id) REFERENCES l001 (lead_id) ON DELETE CASCADE
);
COMMENT ON TABLE l006 IS 'Lead Address Details: Address particulars, house or flat number, street address, locality, city, state and PIN code for a lead.';
-- 4. Optional dedicated credit facilities tables
CREATE TABLE IF NOT EXISTS l007_existing_facilities (
    id BIGSERIAL PRIMARY KEY,
    lead_id VARCHAR(50) NOT NULL REFERENCES l001(lead_id) ON DELETE CASCADE,
    bank_name VARCHAR(150),
    account_number VARCHAR(100),
    loan_type VARCHAR(100),
    credit_limit NUMERIC(15,2),
    emi NUMERIC(15,2),
    outstanding NUMERIC(15,2),
    dpd VARCHAR(50),
    account_status VARCHAR(50),
    statement_requested BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);


CREATE TABLE IF NOT EXISTS l008_other_facilities (
    id BIGSERIAL PRIMARY KEY,
    lead_id VARCHAR(50) NOT NULL REFERENCES l001(lead_id) ON DELETE CASCADE,
    bank_name VARCHAR(150),
    account_number VARCHAR(100),
    loan_type VARCHAR(100),
    credit_limit NUMERIC(15,2),
    emi NUMERIC(15,2),
    outstanding NUMERIC(15,2),
    dpd VARCHAR(50),
    account_status VARCHAR(50),
    statement_requested BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS p001 (
    id                              BIGSERIAL PRIMARY KEY,
    code                            VARCHAR(50) NOT NULL,
    name                            VARCHAR(150) NOT NULL,
    description                     VARCHAR(500),
    status                          VARCHAR(20),
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP,
    created_by                      BIGINT,
    updated_by                      BIGINT,
    CONSTRAINT uq_p001_code UNIQUE (code),
    CONSTRAINT fk_p001_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_p001_updated_by FOREIGN KEY (updated_by) REFERENCES u001 (id)
);

CREATE TABLE IF NOT EXISTS p002 (
    product_id                      BIGINT NOT NULL,
    emi                             VARCHAR(50),
    emi_type                        VARCHAR(50),
    emi_fixed_date                  VARCHAR(20) DEFAULT 'No',
    min_amount                      NUMERIC(15,2),
    max_amount                      NUMERIC(15,2),
    interest_rate_percent           NUMERIC(8,4),
    min_tenure_months               INT,
    max_tenure_months               INT,
    int_base_method                 VARCHAR(50),
    no_of_days_in_year              INT,
    processing_fee_percent          NUMERIC(8,4),
    CONSTRAINT pk_p002 PRIMARY KEY (product_id),
    CONSTRAINT fk_p002_product_id FOREIGN KEY (product_id) REFERENCES p001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS p003 (
    product_id                      BIGINT NOT NULL,
    penalty                         BOOLEAN DEFAULT FALSE,
    penalty_percent                 NUMERIC(8,4),
    interest_calculation_frequency  VARCHAR(50),
    interest_calculation_mode       VARCHAR(50),
    CONSTRAINT pk_p003 PRIMARY KEY (product_id),
    CONSTRAINT fk_p003_product_id FOREIGN KEY (product_id) REFERENCES p001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS p004 (
    id                              BIGSERIAL PRIMARY KEY,
    product_id                      BIGINT NOT NULL,
    sr_no                           VARCHAR(50),
    power                           VARCHAR(100) NOT NULL,
    amount_limit                    NUMERIC(15,2) NOT NULL,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP,
    CONSTRAINT fk_p004_product_id FOREIGN KEY (product_id) REFERENCES p001 (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_p004_product_id ON p004 (product_id);

CREATE TABLE IF NOT EXISTS a001 (
    id                              BIGSERIAL PRIMARY KEY,
    application_number              VARCHAR(40) NOT NULL,
    customer_id                     BIGINT NOT NULL,
    branch_id                       BIGINT,
    product_code                    VARCHAR(50) NOT NULL,
    status                          VARCHAR(40),
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP,
    created_by                      BIGINT,
    updated_by                      BIGINT,
    CONSTRAINT uq_a001_application_number UNIQUE (application_number),
    CONSTRAINT fk_a001_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id),
    CONSTRAINT fk_a001_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_a001_updated_by FOREIGN KEY (updated_by) REFERENCES u001 (id),
    CONSTRAINT fk_a001_branch_id FOREIGN KEY (branch_id) REFERENCES b001 (id),
    CONSTRAINT fk_a001_product_code FOREIGN KEY (product_code) REFERENCES p001 (code)
);

CREATE TABLE IF NOT EXISTS a002 (
    application_id                  BIGINT NOT NULL,
    total_monthly_income            NUMERIC(15,2),
    total_monthly_expenses          NUMERIC(15,2),
    net_monthly_income              NUMERIC(15,2),
    existing_monthly_debt           NUMERIC(15,2),
    cibil_score                     INT,
    collateral_value                NUMERIC(15,2),
    emi                             NUMERIC(15,2),
    dti_ratio                       NUMERIC(8,4),
    CONSTRAINT pk_a002 PRIMARY KEY (application_id),
    CONSTRAINT fk_a002_application_id FOREIGN KEY (application_id) REFERENCES a001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS a003 (
    application_id                  BIGINT NOT NULL,
    applied_amount                  NUMERIC(15,2),
    interest_rate                   NUMERIC(8,4),
    tenure_months                   INT,
    max_eligible_amount             NUMERIC(15,2),
    eligibility_status              VARCHAR(40),
    sanctioned_amount               NUMERIC(15,2),
    kyc_status                      VARCHAR(30),
    CONSTRAINT pk_a003 PRIMARY KEY (application_id),
    CONSTRAINT fk_a003_application_id FOREIGN KEY (application_id) REFERENCES a001 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS a004 (
    application_id                  BIGINT NOT NULL,
    maker_user_id                   BIGINT,
    checker_user_id                 BIGINT,
    checker_remarks                 VARCHAR(500),
    approved_at                     TIMESTAMP,
    CONSTRAINT pk_a004 PRIMARY KEY (application_id),
    CONSTRAINT fk_a004_application_id FOREIGN KEY (application_id) REFERENCES a001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_a004_maker_user_id FOREIGN KEY (maker_user_id) REFERENCES u001 (id),
    CONSTRAINT fk_a004_checker_user_id FOREIGN KEY (checker_user_id) REFERENCES u001 (id)
);

CREATE TABLE IF NOT EXISTS a005 (
    id                              BIGSERIAL PRIMARY KEY,
    loan_application_id             BIGINT NOT NULL,
    item_type                       VARCHAR(20),
    source                          VARCHAR(100),
    monthly_amount                  NUMERIC(15,2),
    CONSTRAINT fk_a005_loan_application_id FOREIGN KEY (loan_application_id) REFERENCES a001 (id) ON DELETE CASCADE
);

CREATE INDEX idx_r003_permission_id ON r003 (permission_id);
CREATE INDEX idx_r004_role_id ON r004 (role_id);
CREATE INDEX idx_r004_created_by ON r004 (created_by);
CREATE INDEX idx_r005_created_by ON r005 (created_by);
CREATE INDEX idx_m001_created_by ON m001 (created_by);
CREATE INDEX idx_m002_created_by ON m002 (created_by);
CREATE INDEX idx_r004_modified_by ON r004 (modified_by);
CREATE INDEX idx_r005_modified_by ON r005 (modified_by);
CREATE INDEX idx_m001_modified_by ON m001 (modified_by);
CREATE INDEX idx_m002_modified_by ON m002 (modified_by);
CREATE INDEX idx_r005_permission_code ON r005 (permission_code);
CREATE INDEX idx_u002_role_id ON u002 (role_id);
CREATE INDEX idx_u003_login_branch_id ON u003 (login_branch_id);
CREATE INDEX idx_u003_created_by ON u003 (created_by);
CREATE INDEX idx_u003_verified_by ON u003 (verified_by);
CREATE INDEX idx_u003_modified_by ON u003 (modified_by);
CREATE INDEX idx_u004_branch_id ON u004 (branch_id);
CREATE INDEX idx_u005_user_id ON u005 (user_id);
CREATE INDEX idx_u006_user_id ON u006 (user_id);
CREATE INDEX idx_u006_created_by ON u006 (created_by);
CREATE INDEX idx_u006_verified_by ON u006 (verified_by);
CREATE INDEX idx_u007_user_id ON u007 (user_id);
CREATE INDEX idx_u008_user_id ON u008 (user_id);
CREATE INDEX idx_u009_staff_user_id ON u009 (staff_user_id);
CREATE INDEX idx_c003_customer_id ON c003 (customer_id);
CREATE INDEX idx_c001_branch_id ON c001 (branch_id);
-- Customer list filters on onboarding status and sorts by newest first.
CREATE INDEX idx_c001_onboarding_status_created_at ON c001 (onboarding_status, created_at DESC);
CREATE INDEX idx_c001_submitted_by ON c001 (submitted_by);
CREATE INDEX idx_m002_lookup_type_code ON m002 (lookup_type_code);
CREATE INDEX idx_m003_lookup_type_code ON m003 (lookup_type_code);
CREATE INDEX idx_m003_permission_code ON m003 (permission_code);
CREATE INDEX idx_a001_customer_id ON a001 (customer_id);
CREATE INDEX idx_a001_branch_id ON a001 (branch_id);
CREATE INDEX idx_a001_product_code ON a001 (product_code);
CREATE INDEX idx_a004_maker_user_id ON a004 (maker_user_id);
CREATE INDEX idx_a004_checker_user_id ON a004 (checker_user_id);
CREATE INDEX idx_a005_loan_application_id ON a005 (loan_application_id);
CREATE INDEX idx_p001_created_by ON p001 (created_by);
CREATE INDEX idx_p001_updated_by ON p001 (updated_by);
CREATE INDEX idx_a001_created_by ON a001 (created_by);
CREATE INDEX idx_a001_updated_by ON a001 (updated_by);

CREATE OR REPLACE VIEW v_users AS
SELECT
    u001.id,
    u001.emp_no,
    u001.username,
    u001.password_hash,
    u001.status,
    u002.first_name,
    u002.middle_name,
    u002.last_name,
    u002.dob,
    u002.email,
    u002.mobile,
    u002.gender,
    u002.designation,
    u002.role_id,
    u003.system_role,
    u003.login_branch_id,
    b001.code AS branch_code,
    b001.name AS branch_name,
    b001.bank_name,
    b001.rbi_license_no,
    b001.city AS branch_city,
    b001.state AS branch_state,
    u003.login_time,
    u003.logout_time,
    u003.inactive_session_timeout,
    u001.no_of_bad_logins,
    u001.last_login_date,
    u001.last_login_time,
    u001.is_active,
    u003.created_by,
    u003.created_at,
    u003.verified_by,
    u003.verified_date,
    u003.modified_by,
    u003.updated_at,
    u004.title
FROM u001
LEFT JOIN u002 ON u002.user_id = u001.id
LEFT JOIN u003 ON u003.user_id = u001.id
LEFT JOIN u004 ON u004.user_id = u001.id
LEFT JOIN b001 ON b001.id = COALESCE(u004.branch_id, u003.login_branch_id)
;

CREATE OR REPLACE VIEW v_customers AS
SELECT
    c001.id,
    c001.customer_code,
    c001.branch_id,
    b001.code AS branch_code,
    b001.name AS branch_name,
    c002.first_name,
    c002.middle_name,
    c002.last_name,
    c002.email,
    c002.phone,
    c001.password_hash,
    c001.is_active,
    c001.last_login_at,
    c001.created_at,
    c001.updated_at
FROM c001
LEFT JOIN c002 ON c002.customer_id = c001.id
LEFT JOIN b001 ON b001.id = c001.branch_id
;

CREATE OR REPLACE VIEW v_leads AS
SELECT
    l001.lead_id,
    l001.customer_id,
    l001.lead_status,
    l001.assigned_user_id,
    l001.assignment_timestamp,
    l001.assigned_by,
    l002.first_name_business_name,
    l002.dob,
    l002.customer_type,
    l003.pan_number,
    l003.pan_validation_status,
    l003.aadhaar_number,
    l003.aadhaar_validation_status,
    l002.residential_status,
    l002.gender,
    l002.marital_status,
    l003.passport_number,
    l003.passport_expiry_date,
    l003.dedupe_status,
    l003.blacklist_status,
    l004.loan_product_type,
    l004.loan_amount,
    l004.purpose_of_loan,
    l004.tenure,
    l004.number_of_instalments,
    l004.emi,
    l004.interest_rate,
    l004.total_interest,
    l004.security_amount,
    l005.employment_type,
    l005.annual_income,
    l005.designation,
    l005.employer_business_name,
    l005.location,
   -- l005.state,
    l005.take_home_pay,
    l005.deductions_or_emis_payable,
    l005.bank_name,
    l005.primary_bank_account,
    l005.account_statement_consent,
    l005.cibil_liability_check,
    l005.debt_to_income_ratio,
    l005.loan_to_value_ratio,
    l005.debt_service_coverage_ratio,
    l005.net_disposable_income,
    l005.emi_bounce,
    l005.emi_bounce_count,
    l001.lead_acquisition_channel,
    l001.date,
    l001.sourcing_agent_partner_id,
    l001.agent_partner_name,
    l002.last_name,
    l002.email_address,
    l002.pin_code,
    l002.mobile_number,
    l002.number_of_dependents,
    l004.down_payment_collateral,
    l001.created_at,
    l001.updated_at,
    l006.particulars,
    l006.house_flat_no,
    l006.street_address,
    l006.locality,
    l006.city,
    l006.state,
    l006.pincode
FROM l001
LEFT JOIN l002 ON l002.lead_id = l001.lead_id
LEFT JOIN l003 ON l003.lead_id = l001.lead_id
LEFT JOIN l004 ON l004.lead_id = l001.lead_id
LEFT JOIN l005 ON l005.lead_id = l001.lead_id
LEFT JOIN l006 ON l006.lead_id = l001.lead_id
;

CREATE OR REPLACE VIEW v_loan_products AS
SELECT
    p001.id,
    p001.code,
    p001.name,
    p001.description,
    p002.emi,
    p002.emi_type,
    p002.emi_fixed_date,
    p002.min_amount,
    p002.max_amount,
    p002.interest_rate_percent,
    p002.min_tenure_months,
    p002.max_tenure_months,
    p002.int_base_method,
    p002.no_of_days_in_year,
    p003.penalty,
    p003.penalty_percent,
    p003.interest_calculation_frequency,
    p003.interest_calculation_mode,
    p002.processing_fee_percent,
    p001.status,
    p001.created_at,
    p001.updated_at,
    p001.created_by,
    p001.updated_by
FROM p001
LEFT JOIN p002 ON p002.product_id = p001.id
LEFT JOIN p003 ON p003.product_id = p001.id
;

CREATE OR REPLACE VIEW v_loan_applications AS
SELECT
    a001.id,
    a001.application_number,
    a001.customer_id,
    a001.branch_id,
    b001.code AS branch_code,
    b001.name AS branch_name,
    a001.product_code,
    a003.applied_amount,
    a003.interest_rate,
    a003.tenure_months,
    a002.total_monthly_income,
    a002.total_monthly_expenses,
    a002.net_monthly_income,
    a002.existing_monthly_debt,
    a002.cibil_score,
    a002.collateral_value,
    a002.emi,
    a002.dti_ratio,
    a003.max_eligible_amount,
    a003.eligibility_status,
    a003.sanctioned_amount,
    a003.kyc_status,
    a004.maker_user_id,
    a004.checker_user_id,
    a001.status,
    a004.checker_remarks,
    a004.approved_at,
    a001.created_at,
    a001.updated_at,
    a001.created_by,
    a001.updated_by
FROM a001
LEFT JOIN a002 ON a002.application_id = a001.id
LEFT JOIN a003 ON a003.application_id = a001.id
LEFT JOIN a004 ON a004.application_id = a001.id
LEFT JOIN b001 ON b001.id = a001.branch_id
;

-- =====================================================================
-- GOLD LOAN ACCOUNT MODULE
-- New tables only. Existing bank tables above are intentionally unchanged.
-- Reuses customer, branch, product, user and lookup masters wherever possible.
-- =====================================================================
-- g008: Gold Loan Valuer Master
CREATE TABLE g008 (
    id                  BIGSERIAL PRIMARY KEY,
    valuer_code         VARCHAR(50) NOT NULL,
    valuer_description  VARCHAR(150),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_g008_valuer_code UNIQUE (valuer_code)
);


-- g009: Gold Loan Scheme Master
CREATE TABLE g009 (
    id                  BIGSERIAL PRIMARY KEY,
    scheme_code         VARCHAR(50) NOT NULL,
    scheme_description  VARCHAR(200),
    product_id          BIGINT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_g009_scheme_code UNIQUE (scheme_code),
    CONSTRAINT fk_g009_product_id
        FOREIGN KEY (product_id) REFERENCES p001 (id)
);


-- g010: Gold Rate Master
CREATE TABLE g010 (
    id                  BIGSERIAL PRIMARY KEY,
    record_id           VARCHAR(50) NOT NULL,
    effective_date      DATE NOT NULL,
    effective_time      TIME,
    rate_24k            NUMERIC(15,2) NOT NULL,
    rate_22k            NUMERIC(15,2) NOT NULL,
    rate_18k            NUMERIC(15,2),
    currency            VARCHAR(10) DEFAULT 'INR',
    unit                VARCHAR(20) DEFAULT 'GRAM',
    status              VARCHAR(30) DEFAULT 'Active',
    status_lookup_id    BIGINT,
    description         VARCHAR(255),
    created_by          BIGINT,
    created_by_name     VARCHAR(100),
    created_by_emp_id   VARCHAR(50),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by         BIGINT,
    modified_by_name    VARCHAR(100),
    modified_by_emp_id  VARCHAR(50),
    updated_at          TIMESTAMP,
    verified_by         BIGINT,
    verified_by_name    VARCHAR(100),
    verified_by_emp_id  VARCHAR(50),
    verified_at         TIMESTAMP,
    CONSTRAINT uq_g010_record_id UNIQUE (record_id),
    CONSTRAINT fk_g010_status_lookup
        FOREIGN KEY (status_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g010_created_by
        FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_g010_modified_by
        FOREIGN KEY (modified_by) REFERENCES u001 (id),
    CONSTRAINT fk_g010_verified_by
        FOREIGN KEY (verified_by) REFERENCES u001 (id)
);


-- ============================================================================
-- GOLD LOAN ACCOUNT TABLES
-- ============================================================================

-- g001: Gold Loan Account Core
CREATE TABLE g001 (
    id                          BIGSERIAL PRIMARY KEY,
    loan_account_number         VARCHAR(50) NOT NULL,
    effective_date              DATE,
    account_status_lookup_id    BIGINT,
    last_transaction_date       DATE,
    customer_id                 BIGINT NOT NULL,
    branch_id                   BIGINT,
    product_id                  BIGINT,
    title_lookup_id             BIGINT,
    scheme_id                   BIGINT,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    CONSTRAINT uq_g001_loan_account_number UNIQUE (loan_account_number),
    CONSTRAINT fk_g001_customer_id
        FOREIGN KEY (customer_id) REFERENCES c001 (id),
    CONSTRAINT fk_g001_branch_id
        FOREIGN KEY (branch_id) REFERENCES b001 (id),
    CONSTRAINT fk_g001_product_id
        FOREIGN KEY (product_id) REFERENCES p001 (id),
    CONSTRAINT fk_g001_account_status_lookup
        FOREIGN KEY (account_status_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g001_title_lookup
        FOREIGN KEY (title_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g001_scheme_id
        FOREIGN KEY (scheme_id) REFERENCES g009 (id)
);


-- g002: Gold Loan Sanction and Limits
CREATE TABLE g002 (
    loan_account_id                 BIGINT PRIMARY KEY,
    mode_of_operation_lookup_id     BIGINT,
    limit_amount                    NUMERIC(15,2),
    margin_percent                  NUMERIC(8,4),
    limit_available_for_sanction    NUMERIC(15,2),
    sanction_limit                  NUMERIC(15,2),
    tenure_months                   INT,
    interest_rate_percent           NUMERIC(8,4),
    sanction_authority              VARCHAR(150),
    number_of_installments          INT,
    sanctioned_date                 DATE,
    emi_lookup_id                   BIGINT,
    installment_amount              NUMERIC(15,2),
    expiry_date                     DATE,
    purpose_of_loan_lookup_id       BIGINT,
    sector_code                     VARCHAR(50),
    women_sector_lookup_id          BIGINT,
    term_code                       VARCHAR(50),
    sub_sector_code                 VARCHAR(50),
    name_type_lookup_id             BIGINT,
    CONSTRAINT fk_g002_loan_account_id
        FOREIGN KEY (loan_account_id) REFERENCES g001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_g002_mode_lookup
        FOREIGN KEY (mode_of_operation_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g002_emi_lookup
        FOREIGN KEY (emi_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g002_purpose_lookup
        FOREIGN KEY (purpose_of_loan_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g002_women_sector_lookup
        FOREIGN KEY (women_sector_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g002_name_type_lookup
        FOREIGN KEY (name_type_lookup_id) REFERENCES m002 (id)
);


-- g003: Gold Loan Repayment Terms
CREATE TABLE g003 (
    loan_account_id                 BIGINT PRIMARY KEY,
    repayment_type_lookup_id        BIGINT,
    customer_sourced_lookup_id      BIGINT,
    repayment_start_date            DATE,
    interest_payment_lookup_id      BIGINT,
    account_closure_lookup_id       BIGINT,
    penal_interest_percent          NUMERIC(8,4),
    CONSTRAINT fk_g003_loan_account_id
        FOREIGN KEY (loan_account_id) REFERENCES g001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_g003_repayment_type_lookup
        FOREIGN KEY (repayment_type_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g003_customer_sourced_lookup
        FOREIGN KEY (customer_sourced_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g003_interest_payment_lookup
        FOREIGN KEY (interest_payment_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g003_account_closure_lookup
        FOREIGN KEY (account_closure_lookup_id) REFERENCES m002 (id)
);


-- g004: Gold Loan Fees and Charges
CREATE TABLE g004 (
    loan_account_id     BIGINT PRIMARY KEY,
    processing_fees     NUMERIC(15,2),
    valuation_charges   NUMERIC(15,2),
    CONSTRAINT fk_g004_loan_account_id
        FOREIGN KEY (loan_account_id) REFERENCES g001 (id) ON DELETE CASCADE
);


-- g005: Security / Collateral Header
CREATE TABLE g005 (
    id                          BIGSERIAL PRIMARY KEY,
    loan_account_id             BIGINT NOT NULL,
    security_id                 VARCHAR(60) NOT NULL,
    security_type_lookup_id     BIGINT,
    CONSTRAINT uq_g005_security_id UNIQUE (security_id),
    CONSTRAINT fk_g005_loan_account_id
        FOREIGN KEY (loan_account_id) REFERENCES g001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_g005_security_type_lookup
        FOREIGN KEY (security_type_lookup_id) REFERENCES m002 (id)
);


-- g006: Ornament Inventory Items
-- One security can contain multiple ornament/inventory items.
CREATE TABLE g006 (
    id                      BIGSERIAL PRIMARY KEY,
    security_id             BIGINT NOT NULL,
    inventory_item_id       VARCHAR(60) NOT NULL,
    ornament_description    VARCHAR(255),
    gross_weight            NUMERIC(12,3),
    net_weight              NUMERIC(12,3),
    appraisal_date          DATE,
    purity_lookup_id        BIGINT,
    rate                    NUMERIC(15,2),
    amount                  NUMERIC(15,2),
    valuer_code             VARCHAR(50),
    gold_rate_id            BIGINT,
    CONSTRAINT uq_g006_inventory_item_id UNIQUE (inventory_item_id),
    CONSTRAINT fk_g006_security_id
        FOREIGN KEY (security_id) REFERENCES g005 (id) ON DELETE CASCADE,
    CONSTRAINT fk_g006_purity_lookup
        FOREIGN KEY (purity_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g006_valuer_code
        FOREIGN KEY (valuer_code) REFERENCES g008 (valuer_code),
    CONSTRAINT fk_g006_gold_rate_id
        FOREIGN KEY (gold_rate_id) REFERENCES g010 (id)
);


-- g007: Disbursement Details
CREATE TABLE g007 (
    id                              BIGSERIAL PRIMARY KEY,
    loan_account_id                 BIGINT NOT NULL UNIQUE,
    account_number                  VARCHAR(50),
    mode_of_disbursement_lookup_id  BIGINT,
    type_of_account_lookup_id       BIGINT,
    disbursement_type_lookup_id     BIGINT,
    bank_name                       VARCHAR(150),
    disbursement_amount             NUMERIC(15,2),
    ifsc_code                       VARCHAR(11),
    CONSTRAINT fk_g007_loan_account_id
        FOREIGN KEY (loan_account_id) REFERENCES g001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_g007_mode_lookup
        FOREIGN KEY (mode_of_disbursement_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g007_account_type_lookup
        FOREIGN KEY (type_of_account_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g007_disbursement_type_lookup
        FOREIGN KEY (disbursement_type_lookup_id) REFERENCES m002 (id)
);


-- g011: Gold Rate Activity and Complete Audit Trail
CREATE TABLE g011 (
    id                  BIGSERIAL PRIMARY KEY,
    gold_rate_id        BIGINT,
    record_id           VARCHAR(50) NOT NULL,
    activity_date       DATE NOT NULL,
    activity_time       TIME NOT NULL,
    module              VARCHAR(100) NOT NULL DEFAULT 'Gold Rate Master',
    action              VARCHAR(50) NOT NULL,
    field_name          VARCHAR(100),
    old_value           VARCHAR(100),
    new_value           VARCHAR(100),
    created_by          BIGINT,
    created_by_name     VARCHAR(100),
    created_by_emp_id   VARCHAR(50),
    modified_by         BIGINT,
    modified_by_name    VARCHAR(100),
    modified_by_emp_id  VARCHAR(50),
    verified_by         BIGINT,
    verified_by_name    VARCHAR(100) DEFAULT '-',
    verified_by_emp_id  VARCHAR(50) DEFAULT '-',
    status              VARCHAR(30) DEFAULT 'Pending',
    status_lookup_id    BIGINT,
    description         TEXT,
    ip_address          VARCHAR(50),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_g011_gold_rate_id
        FOREIGN KEY (gold_rate_id) REFERENCES g010 (id) ON DELETE CASCADE,
    CONSTRAINT fk_g011_status_lookup
        FOREIGN KEY (status_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_g011_created_by
        FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_g011_modified_by
        FOREIGN KEY (modified_by) REFERENCES u001 (id),
    CONSTRAINT fk_g011_verified_by
        FOREIGN KEY (verified_by) REFERENCES u001 (id)
);


-- Lookup values are resolved through the existing m001/m002 lookup tables.
-- These indexes support account, security, inventory and disbursement joins.
CREATE INDEX idx_g001_customer_id ON g001 (customer_id);
CREATE INDEX idx_g001_branch_id ON g001 (branch_id);
CREATE INDEX idx_g001_product_id ON g001 (product_id);
CREATE INDEX idx_g001_scheme_id ON g001 (scheme_id);

CREATE INDEX idx_g005_loan_account_id ON g005 (loan_account_id);

CREATE INDEX idx_g006_security_id ON g006 (security_id);
CREATE INDEX idx_g006_valuer_code ON g006 (valuer_code);
CREATE INDEX idx_g006_gold_rate_id ON g006 (gold_rate_id);

CREATE INDEX idx_g007_loan_account_id ON g007 (loan_account_id);

CREATE INDEX idx_g010_effective_date ON g010 (effective_date);

CREATE INDEX idx_g011_record_id ON g011 (record_id);
CREATE INDEX idx_g011_gold_rate_id ON g011 (gold_rate_id);
CREATE INDEX idx_g011_activity_date ON g011 (activity_date);
CREATE INDEX idx_g011_status ON g011 (status);

-- One row per ornament/inventory item. Account, customer, branch, product,
-- scheme, valuer and lookup descriptions are displayed through joins.



-- ============================================================================
-- COMPATIBILITY VIEWS
-- ============================================================================

CREATE OR REPLACE VIEW gold_rate_master AS
SELECT * FROM g010;

CREATE OR REPLACE VIEW gold_rate_activity AS
SELECT * FROM g011;


-- ============================================================================
-- GOLD LOAN ACCOUNT VIEW
-- One result row per ornament/inventory item.
-- Account, customer, branch, product, scheme, valuer and lookup descriptions
-- are displayed through joins.
-- ============================================================================

CREATE OR REPLACE VIEW v_gold_loan_account AS
SELECT
    g001.id AS loan_account_id,
    g001.loan_account_number,
    g001.effective_date,
    account_status.sub_type_description AS account_status,
    g001.last_transaction_date,
    c001.customer_code AS customer_id,
    b001.code AS branch_code,
    b001.name AS branch_name,
    title.sub_type_description AS title,
    c002.first_name,
    c002.middle_name,
    c002.last_name,
    p001.code AS product_code,
    p001.description AS product_description,
    g009.scheme_code,
    g009.scheme_description,
    operation_mode.sub_type_description AS mode_of_operation,
    g005.security_id,
    security_type.sub_type_description AS security_type,
    g008.valuer_code,
    g008.valuer_description,
    g006.inventory_item_id,
    g006.ornament_description,
    g006.gross_weight,
    g006.net_weight,
    g006.appraisal_date,
    purity.sub_type_description AS purity,
    g006.rate,
    g006.amount AS valuation_amount,
    g002.limit_amount,
    g002.margin_percent,
    g002.limit_available_for_sanction,
    g002.sanction_limit,
    g002.tenure_months,
    g002.interest_rate_percent,
    g002.sanction_authority,
    g002.number_of_installments,
    g002.sanctioned_date,
    emi_flag.sub_type_description AS emi,
    g002.installment_amount,
    g002.expiry_date,
    repayment_type.sub_type_description AS repayment_type,
    customer_sourced.sub_type_description AS customer_sourced,
    g003.repayment_start_date,
    interest_payment.sub_type_description AS interest_payment,
    account_closure.sub_type_description AS account_closure,
    g004.processing_fees,
    g004.valuation_charges,
    g003.penal_interest_percent,
    purpose.sub_type_description AS purpose_of_loan,
    g002.sector_code,
    women_sector.sub_type_description AS women_sector,
    g002.term_code,
    g002.sub_sector_code,
    name_type.sub_type_description AS name_type,
    g007.account_number AS disbursement_account_number,
    disbursement_mode.sub_type_description AS mode_of_disbursement,
    account_type.sub_type_description AS type_of_account,
    disbursement_type.sub_type_description AS disbursement_type,
    g007.bank_name AS disbursement_bank_name,
    g007.disbursement_amount,
    g007.ifsc_code,
    c002.phone AS mobile_number
FROM g001
LEFT JOIN c001
    ON c001.id = g001.customer_id
LEFT JOIN c002
    ON c002.customer_id = c001.id
LEFT JOIN b001
    ON b001.id = g001.branch_id
LEFT JOIN p001
    ON p001.id = g001.product_id
LEFT JOIN g009
    ON g009.id = g001.scheme_id
LEFT JOIN g002
    ON g002.loan_account_id = g001.id
LEFT JOIN g003
    ON g003.loan_account_id = g001.id
LEFT JOIN g004
    ON g004.loan_account_id = g001.id
LEFT JOIN g005
    ON g005.loan_account_id = g001.id
LEFT JOIN g006
    ON g006.security_id = g005.id
LEFT JOIN g008
    ON g008.valuer_code = g006.valuer_code
LEFT JOIN g007
    ON g007.loan_account_id = g001.id
LEFT JOIN m002 account_status
    ON account_status.id = g001.account_status_lookup_id
LEFT JOIN m002 title
    ON title.id = g001.title_lookup_id
LEFT JOIN m002 operation_mode
    ON operation_mode.id = g002.mode_of_operation_lookup_id
LEFT JOIN m002 security_type
    ON security_type.id = g005.security_type_lookup_id
LEFT JOIN m002 purity
    ON purity.id = g006.purity_lookup_id
LEFT JOIN m002 emi_flag
    ON emi_flag.id = g002.emi_lookup_id
LEFT JOIN m002 repayment_type
    ON repayment_type.id = g003.repayment_type_lookup_id
LEFT JOIN m002 customer_sourced
    ON customer_sourced.id = g003.customer_sourced_lookup_id
LEFT JOIN m002 interest_payment
    ON interest_payment.id = g003.interest_payment_lookup_id
LEFT JOIN m002 account_closure
    ON account_closure.id = g003.account_closure_lookup_id
LEFT JOIN m002 purpose
    ON purpose.id = g002.purpose_of_loan_lookup_id
LEFT JOIN m002 women_sector
    ON women_sector.id = g002.women_sector_lookup_id
LEFT JOIN m002 name_type
    ON name_type.id = g002.name_type_lookup_id
LEFT JOIN m002 disbursement_mode
    ON disbursement_mode.id = g007.mode_of_disbursement_lookup_id
LEFT JOIN m002 account_type
    ON account_type.id = g007.type_of_account_lookup_id
LEFT JOIN m002 disbursement_type
    ON disbursement_type.id = g007.disbursement_type_lookup_id;


-- ============================================================================
-- GOLD RATE ACTIVITY VIEW
-- ============================================================================

CREATE OR REPLACE VIEW v_gold_rate_activity AS
SELECT
    g011.id,
    g011.activity_date,
    TO_CHAR(g011.activity_date, 'DD Mon YYYY') AS formatted_date,
    g011.activity_time,
    TO_CHAR(g011.activity_time, 'HH12:MI AM') AS formatted_time,
    g011.module,
    g011.record_id,
    g011.action,
    g011.field_name AS field,
    g011.old_value,
    g011.new_value,
    COALESCE(
        TRIM(CONCAT(cu_prof.first_name, ' ', cu_prof.last_name)),
        cu.username,
        g011.created_by_name
    ) AS created_by,
    COALESCE(cu.emp_no, g011.created_by_emp_id) AS created_by_id,
    COALESCE(
        TRIM(CONCAT(mu_prof.first_name, ' ', mu_prof.last_name)),
        mu.username,
        g011.modified_by_name
    ) AS modified_by,
    COALESCE(mu.emp_no, g011.modified_by_emp_id) AS modified_by_id,
    COALESCE(
        TRIM(CONCAT(vu_prof.first_name, ' ', vu_prof.last_name)),
        vu.username,
        g011.verified_by_name,
        '-'
    ) AS verified_by,
    COALESCE(vu.emp_no, g011.verified_by_emp_id, '-') AS verified_by_id,
    g011.status,
    g011.description,
    g011.ip_address AS ip,
    g010.rate_24k,
    g010.rate_22k,
    g010.rate_18k
FROM g011
LEFT JOIN g010
    ON g010.record_id = g011.record_id
LEFT JOIN u001 cu
    ON cu.id = g011.created_by
LEFT JOIN u002 cu_prof
    ON cu_prof.user_id = cu.id
LEFT JOIN u001 mu
    ON mu.id = g011.modified_by
LEFT JOIN u002 mu_prof
    ON mu_prof.user_id = mu.id
LEFT JOIN u001 vu
    ON vu.id = g011.verified_by
LEFT JOIN u002 vu_prof
    ON vu_prof.user_id = vu.id;


-- ============================================================================
-- END OF GOLD LOAN MODULE
-- ============================================================================


-- ============================================================================
-- D001 (Modules), F001 (Features), N001 (Actions) RBAC HIERARCHY
-- ============================================================================

-- 1. Table D001: System Modules
CREATE TABLE IF NOT EXISTS D001 (
    module_id       BIGSERIAL PRIMARY KEY,
    module_name     VARCHAR(100) NOT NULL,
    module_code     VARCHAR(50)  NOT NULL,
    description     VARCHAR(255),
    status          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_d001_module_code UNIQUE (module_code)
);

-- 2. Table F001: Features within Modules
CREATE TABLE IF NOT EXISTS F001 (
    feature_id      BIGSERIAL PRIMARY KEY,
    module_id       BIGINT       NOT NULL,
    feature_name    VARCHAR(100) NOT NULL,
    feature_code    VARCHAR(50)  NOT NULL,
    description     VARCHAR(255),
    status          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_f001_module_id FOREIGN KEY (module_id) REFERENCES D001 (module_id) ON DELETE CASCADE,
    CONSTRAINT uq_f001_module_feature_code UNIQUE (module_id, feature_code)
);

CREATE INDEX IF NOT EXISTS idx_f001_module_id ON F001 (module_id);

-- 3. Table N001: Actions within Features
CREATE TABLE IF NOT EXISTS N001 (
    action_id       BIGSERIAL PRIMARY KEY,
    feature_id      BIGINT       NOT NULL,
    action_name     VARCHAR(100) NOT NULL,
    action_code     VARCHAR(50)  NOT NULL,
    description     VARCHAR(255),
    status          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_n001_feature_id FOREIGN KEY (feature_id) REFERENCES F001 (feature_id) ON DELETE CASCADE,
    CONSTRAINT uq_n001_feature_action_code UNIQUE (feature_id, action_code)
);

CREATE INDEX IF NOT EXISTS idx_n001_feature_id ON N001 (feature_id);

-- 4. View: Hierarchical Module, Feature, and Action Mapping
CREATE OR REPLACE VIEW v_module_feature_actions AS
SELECT 
    d.module_id,
    d.module_code,
    d.module_name,
    f.feature_id,
    f.feature_code,
    f.feature_name,
    n.action_id,
    n.action_code,
    n.action_name,
    CONCAT(d.module_code, '.', f.feature_code, '.', n.action_code) AS full_permission_code,
    (d.status AND f.status AND n.status) AS is_active
FROM D001 d
JOIN F001 f ON d.module_id = f.module_id
JOIN N001 n ON f.feature_id = n.feature_id;



-- =====================================================================
-- CUSTOMER ONBOARDING MODULE  (append this block to the END of V1)
--
-- Paste at the end of your V1 initial schema file, after the RBAC section.
-- Everything it references (c001, c002, l001, m002, u001, b001) is created
-- earlier in V1. Existing tables are NOT altered.
--
-- Conventions (same as the rest of V1):
--   * one-to-one detail tables use customer_id as PK and FK to c001
--   * one-to-many tables use their own BIGSERIAL id plus customer_id FK
--   * lookup-backed columns store m002.id (*_lookup_id)
--   * PAN / Aadhaar / CKYC / bank account number are stored encrypted (BYTEA)
--   * c001 already has created_at / updated_at, so c004 does not repeat them
--   * every FK column has an index
-- =====================================================================

-- ---------------------------------------------------------------------
-- Customer code generator: backend builds CUS-<year>-<LPAD(nextval,5,'0')>
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS seq_customer_code
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- ---------------------------------------------------------------------
-- c004 : Personal Details (step 1)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c004 (
    customer_id                     BIGINT NOT NULL,
    lead_id                         VARCHAR(50),
    title_lookup_id                 BIGINT,
    gender_lookup_id                BIGINT,
    marital_status_lookup_id        BIGINT,
    customer_type_lookup_id         BIGINT,
    kyc_status_lookup_id            BIGINT,
    dob                             DATE NOT NULL,
    occupation                      VARCHAR(50),
    alternate_contact               VARCHAR(10),
    -- created_at / updated_at already exist on c001 and are NOT repeated here.
    -- The backend must set c001.updated_at on every customer update.
    created_by                      BIGINT,
    modified_by                     BIGINT,
    CONSTRAINT pk_c004 PRIMARY KEY (customer_id),
    CONSTRAINT fk_c004_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_c004_lead_id FOREIGN KEY (lead_id) REFERENCES l001 (lead_id),
    CONSTRAINT fk_c004_title_lookup FOREIGN KEY (title_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c004_gender_lookup FOREIGN KEY (gender_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c004_marital_status_lookup FOREIGN KEY (marital_status_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c004_customer_type_lookup FOREIGN KEY (customer_type_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c004_kyc_status_lookup FOREIGN KEY (kyc_status_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c004_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_c004_modified_by FOREIGN KEY (modified_by) REFERENCES u001 (id)
);
COMMENT ON TABLE c004 IS 'Customer Personal Details: lead link, title, gender, marital status, customer type, KYC status, date of birth, occupation, alternate contact. Age is computed, never stored.';

-- A lead can be converted into only one customer.
CREATE UNIQUE INDEX IF NOT EXISTS uq_c004_lead_id ON c004 (lead_id) WHERE lead_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- c005 : Identity Documents numbers (step 1) - encrypted
-- *_hash columns are keyed hashes (HMAC-SHA256, server secret) used ONLY
-- for duplicate checks, because encrypted values cannot be indexed.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c005 (
    customer_id                     BIGINT NOT NULL,
    pan_number                      BYTEA,
    pan_hash                        VARCHAR(64),
    pan_validation_status           VARCHAR(50),
    aadhaar_number                  BYTEA,
    aadhaar_hash                    VARCHAR(64),
    aadhaar_validation_status       VARCHAR(50),
    ckyc_number                     BYTEA,
    CONSTRAINT pk_c005 PRIMARY KEY (customer_id),
    CONSTRAINT fk_c005_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE
);
COMMENT ON TABLE c005 IS 'Customer Identity: encrypted PAN, Aadhaar and CKYC numbers plus keyed hashes for duplicate detection.';

CREATE UNIQUE INDEX IF NOT EXISTS uq_c005_pan_hash ON c005 (pan_hash) WHERE pan_hash IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_c005_aadhaar_hash ON c005 (aadhaar_hash) WHERE aadhaar_hash IS NOT NULL;

-- ---------------------------------------------------------------------
-- c006 : Family Details (step 1)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c006 (
    customer_id                     BIGINT NOT NULL,
    father_title_lookup_id          BIGINT,
    father_first_name               VARCHAR(80),
    father_middle_name              VARCHAR(80),
    father_last_name                VARCHAR(80),
    mother_title_lookup_id          BIGINT,
    mother_first_name               VARCHAR(80),
    mother_middle_name              VARCHAR(80),
    mother_last_name                VARCHAR(80),
    spouse_title_lookup_id          BIGINT,
    spouse_first_name               VARCHAR(80),
    spouse_middle_name              VARCHAR(80),
    spouse_last_name                VARCHAR(80),
    CONSTRAINT pk_c006 PRIMARY KEY (customer_id),
    CONSTRAINT fk_c006_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_c006_father_title_lookup FOREIGN KEY (father_title_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c006_mother_title_lookup FOREIGN KEY (mother_title_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c006_spouse_title_lookup FOREIGN KEY (spouse_title_lookup_id) REFERENCES m002 (id)
);
COMMENT ON TABLE c006 IS 'Customer Family Details: father/husband, mother and spouse names with titles.';

-- ---------------------------------------------------------------------
-- c007 : Address Details (step 2)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c007 (
    customer_id                     BIGINT NOT NULL,
    residential_status_lookup_id    BIGINT,
    residential_type_lookup_id      BIGINT,
    flat_building_name              VARCHAR(150),
    street_road                     VARCHAR(150),
    landmark                        VARCHAR(150),
    city                            VARCHAR(100),
    state_lookup_id                 BIGINT,
    pincode                         VARCHAR(6),
    CONSTRAINT pk_c007 PRIMARY KEY (customer_id),
    CONSTRAINT fk_c007_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_c007_residential_status_lookup FOREIGN KEY (residential_status_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c007_residential_type_lookup FOREIGN KEY (residential_type_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c007_state_lookup FOREIGN KEY (state_lookup_id) REFERENCES m002 (id)
);
COMMENT ON TABLE c007 IS 'Customer Address Details: residential status/type, flat or building, street, landmark, city, state and PIN code.';

-- ---------------------------------------------------------------------
-- c008 : Employment Details (step 3)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c008 (
    customer_id                     BIGINT NOT NULL,
    employment_type_lookup_id       BIGINT,
    employer_type_lookup_id         BIGINT,
    designation                     VARCHAR(100),
    employer_name                   VARCHAR(150),
    date_of_joining                 DATE,
    qualification                   VARCHAR(100),
    employer_email                  VARCHAR(150),
    listed_status_lookup_id         BIGINT,
    years_of_service                NUMERIC(5,1),
    location                        VARCHAR(100),
    date_of_retirement              DATE,
    CONSTRAINT pk_c008 PRIMARY KEY (customer_id),
    CONSTRAINT fk_c008_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_c008_employment_type_lookup FOREIGN KEY (employment_type_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c008_employer_type_lookup FOREIGN KEY (employer_type_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c008_listed_status_lookup FOREIGN KEY (listed_status_lookup_id) REFERENCES m002 (id)
);
COMMENT ON TABLE c008 IS 'Customer Employment Details. For self employed customers designation = nature of business, employer_name = business or trade name, date_of_joining = business start date, location = business location.';

-- ---------------------------------------------------------------------
-- c009 : Previous Employment (step 3, one customer -> many rows)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c009 (
    id                              BIGSERIAL PRIMARY KEY,
    customer_id                     BIGINT NOT NULL,
    employment_type_lookup_id       BIGINT,
    employer_name                   VARCHAR(150),
    designation                     VARCHAR(100),
    location                        VARCHAR(100),
    date_of_joining                 DATE,
    date_of_relieving               DATE,
    CONSTRAINT fk_c009_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_c009_employment_type_lookup FOREIGN KEY (employment_type_lookup_id) REFERENCES m002 (id)
);
COMMENT ON TABLE c009 IS 'Customer Previous Employment records (salaried customers only).';

-- ---------------------------------------------------------------------
-- c010 : Income Details (step 4)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c010 (
    customer_id                         BIGINT NOT NULL,
    annual_income                       NUMERIC(15,2),
    other_source_income                 NUMERIC(15,2),
    total_gross_income                  NUMERIC(15,2),
    total_net_income                    NUMERIC(15,2),
    income_tax                          NUMERIC(15,2),
    income_tax_filer_status_lookup_id   BIGINT,
    risk_category_lookup_id             BIGINT,
    CONSTRAINT pk_c010 PRIMARY KEY (customer_id),
    CONSTRAINT fk_c010_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_c010_filer_status_lookup FOREIGN KEY (income_tax_filer_status_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c010_risk_category_lookup FOREIGN KEY (risk_category_lookup_id) REFERENCES m002 (id)
);
COMMENT ON TABLE c010 IS 'Customer Income Details: annual income, other income, gross and net income, income tax, filer status and risk category.';

-- ---------------------------------------------------------------------
-- c011 : Bank Account (step 4) - account number encrypted
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c011 (
    customer_id                     BIGINT NOT NULL,
    bank_name                       VARCHAR(150),
    account_number                  BYTEA,
    account_type_lookup_id          BIGINT,
    account_status_lookup_id        BIGINT,
    CONSTRAINT pk_c011 PRIMARY KEY (customer_id),
    CONSTRAINT fk_c011_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_c011_account_type_lookup FOREIGN KEY (account_type_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c011_account_status_lookup FOREIGN KEY (account_status_lookup_id) REFERENCES m002 (id)
);
COMMENT ON TABLE c011 IS 'Customer primary bank account: bank name, encrypted account number, account type and status.';

-- ---------------------------------------------------------------------
-- c012 : Income Tax Filing Details (step 4, one customer -> many rows)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c012 (
    id                              BIGSERIAL PRIMARY KEY,
    customer_id                     BIGINT NOT NULL,
    financial_year                  VARCHAR(7) NOT NULL,
    gross_income                    NUMERIC(15,2),
    tds_paid                        NUMERIC(15,2),
    CONSTRAINT uq_c012_customer_year UNIQUE (customer_id, financial_year),
    CONSTRAINT fk_c012_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE
);
COMMENT ON TABLE c012 IS 'Customer income tax returns per financial year (format 2025-26): gross income and TDS paid. Only for filers.';

-- ---------------------------------------------------------------------
-- c013 : Documents (step 5, one customer -> many rows)
-- Files live on disk / object storage; only metadata is stored here.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c013 (
    id                              BIGSERIAL PRIMARY KEY,
    customer_id                     BIGINT NOT NULL,
    document_type_lookup_id         BIGINT,
    original_file_name              VARCHAR(255) NOT NULL,
    content_type                    VARCHAR(100),
    file_size_bytes                 BIGINT,
    storage_path                    VARCHAR(500) NOT NULL,
    expiry_date                     DATE,
    remarks                         VARCHAR(255),
    is_deleted                      BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_by                     BIGINT,
    uploaded_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by                      BIGINT,
    deleted_at                      TIMESTAMP,
    CONSTRAINT fk_c013_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE,
    CONSTRAINT fk_c013_document_type_lookup FOREIGN KEY (document_type_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_c013_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES u001 (id),
    CONSTRAINT fk_c013_deleted_by FOREIGN KEY (deleted_by) REFERENCES u001 (id)
);
COMMENT ON TABLE c013 IS 'Customer uploaded document metadata (file name, type, size, storage path, expiry, remarks). storage_path is RELATIVE to the configured uploads folder, for example LEAD12345/<uuid>_aadhaar.pdf (folder = lead id, or customer code when there is no lead). Soft delete via is_deleted; the physical file is kept.';

-- ---------------------------------------------------------------------
-- c014 : Manual Loan History (step 6, one customer -> many rows)
-- Loans taken through this system are derived from a001/a003 at read time
-- and are NOT stored here.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c014 (
    id                              BIGSERIAL PRIMARY KEY,
    customer_id                     BIGINT NOT NULL,
    bank_name                       VARCHAR(150),
    account_number                  VARCHAR(30),
    loan_type                       VARCHAR(100),
    total_outstanding               NUMERIC(15,2),
    emi                             NUMERIC(15,2),
    expiry_date                     DATE,
    CONSTRAINT fk_c014_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE
);
COMMENT ON TABLE c014 IS 'Customer loan history entered manually (loans with other banks): bank, account number, loan type, outstanding, EMI, expiry date.';

-- ---------------------------------------------------------------------
-- c015 : References (step 7) - at most 2 per customer
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS c015 (
    id                              BIGSERIAL PRIMARY KEY,
    customer_id                     BIGINT NOT NULL,
    sr_no                           SMALLINT NOT NULL,
    reference_name                  VARCHAR(80) NOT NULL,
    relation                        VARCHAR(50),
    location                        VARCHAR(100),
    contact_no                      VARCHAR(10),
    email                           VARCHAR(150),
    CONSTRAINT uq_c015_customer_sr_no UNIQUE (customer_id, sr_no),
    CONSTRAINT ck_c015_sr_no CHECK (sr_no BETWEEN 1 AND 2),
    CONSTRAINT fk_c015_customer_id FOREIGN KEY (customer_id) REFERENCES c001 (id) ON DELETE CASCADE
);
COMMENT ON TABLE c015 IS 'Customer references (two mandatory contacts for future contact): name, relation, location, contact number and email.';

-- ---------------------------------------------------------------------
-- Indexes on foreign key columns
-- ---------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_c004_title_lookup_id ON c004 (title_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c004_gender_lookup_id ON c004 (gender_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c004_marital_status_lookup_id ON c004 (marital_status_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c004_customer_type_lookup_id ON c004 (customer_type_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c004_kyc_status_lookup_id ON c004 (kyc_status_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c004_created_by ON c004 (created_by);
CREATE INDEX IF NOT EXISTS idx_c004_modified_by ON c004 (modified_by);
CREATE INDEX IF NOT EXISTS idx_c006_father_title_lookup_id ON c006 (father_title_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c006_mother_title_lookup_id ON c006 (mother_title_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c006_spouse_title_lookup_id ON c006 (spouse_title_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c007_residential_status_lookup_id ON c007 (residential_status_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c007_residential_type_lookup_id ON c007 (residential_type_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c007_state_lookup_id ON c007 (state_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c008_employment_type_lookup_id ON c008 (employment_type_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c008_employer_type_lookup_id ON c008 (employer_type_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c008_listed_status_lookup_id ON c008 (listed_status_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c009_customer_id ON c009 (customer_id);
CREATE INDEX IF NOT EXISTS idx_c009_employment_type_lookup_id ON c009 (employment_type_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c010_filer_status_lookup_id ON c010 (income_tax_filer_status_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c010_risk_category_lookup_id ON c010 (risk_category_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c011_account_type_lookup_id ON c011 (account_type_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c011_account_status_lookup_id ON c011 (account_status_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c012_customer_id ON c012 (customer_id);
-- Active (not deleted) documents of a customer by type: document list, auto-replace
-- on upload and the required-documents check on submit.
CREATE INDEX IF NOT EXISTS idx_c013_customer_active_docs
    ON c013 (customer_id, document_type_lookup_id, uploaded_at DESC)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_c013_document_type_lookup_id ON c013 (document_type_lookup_id);
CREATE INDEX IF NOT EXISTS idx_c013_uploaded_by ON c013 (uploaded_by);
CREATE INDEX IF NOT EXISTS idx_c013_deleted_by ON c013 (deleted_by);
CREATE INDEX IF NOT EXISTS idx_c014_customer_id ON c014 (customer_id);
CREATE INDEX IF NOT EXISTS idx_c015_customer_id ON c015 (customer_id);

-- Searches on the customer list page (name, email, mobile)
CREATE INDEX IF NOT EXISTS idx_c002_email ON c002 (email);
CREATE INDEX IF NOT EXISTS idx_c002_phone ON c002 (phone);

-- ---------------------------------------------------------------------
-- v_customer_profile : read view for the customer list page.
-- Deliberately excludes password_hash, encrypted PAN/Aadhaar/CKYC,
-- hashes and the bank account number.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_customer_profile AS
SELECT
    c001.id AS customer_id,
    c001.customer_code,
    c001.branch_id,
    b001.code AS branch_code,
    b001.name AS branch_name,
    c001.is_active,
    c001.created_at,
    c001.updated_at,
    c002.first_name,
    c002.middle_name,
    c002.last_name,
    c002.email,
    c002.phone AS mobile_number,
    c004.lead_id,
    m_title.sub_type_description AS title,
    m_gender.sub_type_description AS gender,
    m_marital.sub_type_description AS marital_status,
    m_ctype.sub_type_description AS customer_type,
    m_kyc.sub_type_description AS kyc_status,
    c004.dob,
    CASE WHEN c004.dob IS NULL THEN NULL
         ELSE EXTRACT(YEAR FROM age(c004.dob))::INT END AS age,
    c004.occupation,
    c004.alternate_contact,
    m_res_status.sub_type_description AS residential_status,
    m_res_type.sub_type_description AS residential_type,
    c007.flat_building_name,
    c007.street_road,
    c007.landmark,
    c007.city,
    m_state.sub_type_description AS state,
    c007.pincode,
    m_emp_type.sub_type_description AS employment_type,
    m_employer_type.sub_type_description AS employer_type,
    c008.designation,
    c008.employer_name,
    c008.date_of_joining,
    c008.qualification,
    c008.employer_email,
    m_listed.sub_type_description AS listed_status,
    c008.years_of_service,
    c008.location AS employment_location,
    c008.date_of_retirement,
    c010.annual_income,
    c010.other_source_income,
    c010.total_gross_income,
    c010.total_net_income,
    c010.income_tax,
    m_filer.sub_type_description AS income_tax_filer_status,
    m_risk.sub_type_description AS risk_category,
    c001.onboarding_status,
    c001.submitted_at
FROM c001
LEFT JOIN b001 ON b001.id = c001.branch_id
LEFT JOIN c002 ON c002.customer_id = c001.id
LEFT JOIN c004 ON c004.customer_id = c001.id
LEFT JOIN c007 ON c007.customer_id = c001.id
LEFT JOIN c008 ON c008.customer_id = c001.id
LEFT JOIN c010 ON c010.customer_id = c001.id
LEFT JOIN m002 m_title ON m_title.id = c004.title_lookup_id
LEFT JOIN m002 m_gender ON m_gender.id = c004.gender_lookup_id
LEFT JOIN m002 m_marital ON m_marital.id = c004.marital_status_lookup_id
LEFT JOIN m002 m_ctype ON m_ctype.id = c004.customer_type_lookup_id
LEFT JOIN m002 m_kyc ON m_kyc.id = c004.kyc_status_lookup_id
LEFT JOIN m002 m_res_status ON m_res_status.id = c007.residential_status_lookup_id
LEFT JOIN m002 m_res_type ON m_res_type.id = c007.residential_type_lookup_id
LEFT JOIN m002 m_state ON m_state.id = c007.state_lookup_id
LEFT JOIN m002 m_emp_type ON m_emp_type.id = c008.employment_type_lookup_id
LEFT JOIN m002 m_employer_type ON m_employer_type.id = c008.employer_type_lookup_id
LEFT JOIN m002 m_listed ON m_listed.id = c008.listed_status_lookup_id
LEFT JOIN m002 m_filer ON m_filer.id = c010.income_tax_filer_status_lookup_id
LEFT JOIN m002 m_risk ON m_risk.id = c010.risk_category_lookup_id;

-- ---------------------------------------------------------------------
-- OPTIONAL (not applied): last safety net against duplicate sub lookup
-- codes inside one lookup. Run only after confirming no duplicates exist
-- (SELECT lookup_type_code, sub_type_code FROM m002
--  GROUP BY 1, 2 HAVING COUNT(*) > 1;)
-- ---------------------------------------------------------------------
-- ALTER TABLE m002 ADD CONSTRAINT uq_m002_lookup_sub UNIQUE (lookup_type_code, sub_type_code);
-- =============================================================================
-- Migration: Rule Engine Module Table, Indexes & View
-- Database: Target Bank Tenant Database (e.g., bank_sbi001)
-- Module: Rule Engine
-- Table: e001
-- View: v_rule_engine
-- =============================================================================

-- =============================================================================
-- Table: e001 (Configurable Business Rules)
-- =============================================================================

CREATE TABLE IF NOT EXISTS e001 (
    id                      BIGSERIAL PRIMARY KEY,
    status                  VARCHAR(20) NOT NULL DEFAULT 'active',
    param                   VARCHAR(100) NOT NULL,
    comparison_operator     VARCHAR(50) NOT NULL,
    from_value              VARCHAR(100),
    to_value                VARCHAR(100),
    val                     VARCHAR(100),
    action_on_match         VARCHAR(50) NOT NULL,
    execution_offset        INT NOT NULL DEFAULT 0,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              BIGINT,
    updated_by              BIGINT,

    CONSTRAINT chk_e001_status
        CHECK (status IN ('active', 'inactive')),

    CONSTRAINT chk_e001_offset
        CHECK (execution_offset >= 0)
);

-- =============================================================================
-- Indexes for Rule Retrieval and Evaluation
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_e001_param
    ON e001 (param);

CREATE INDEX IF NOT EXISTS idx_e001_status
    ON e001 (status);

CREATE INDEX IF NOT EXISTS idx_e001_offset
    ON e001 (execution_offset);

-- =============================================================================
-- View: v_rule_engine
-- Description: Displays rule details along with creator and updater information
-- =============================================================================

CREATE OR REPLACE VIEW v_rule_engine AS
SELECT
    re.id,
    re.status,
    re.param,
    re.comparison_operator,
    re.from_value,
    re.to_value,
    re.val,
    re.action_on_match,
    re.execution_offset,
    re.created_at,
    re.updated_at,
    re.created_by,
    creator.username AS created_by_username,
    re.updated_by,
    updater.username AS updated_by_username
FROM e001 re
LEFT JOIN u001 creator
    ON creator.id = re.created_by
LEFT JOIN u001 updater
    ON updater.id = re.updated_by;

-- =============================================================================
-- End of Rule Engine Module
-- =============================================================================


-- =============================================================================
-- INTEREST RATE MASTER
--
-- A loan product (p001) holds the product RULES; its interest RATES live here
-- as effective-dated rate sheets, so rates can change without editing the
-- product and old rates stay as history.
--
--   i001 : rate sheet header  (one product + one effective date)
--   i002 : rate tiers         (up to amount x up to months -> rates)
--
-- Lifecycle: PENDING (maker) -> APPROVED or REJECTED (checker, a different user).
-- Only APPROVED sheets are used for loans. A sheet that is APPROVED and already
-- in effect can no longer be edited or deleted; rates are changed by creating a
-- new sheet with a later effective date.
-- =============================================================================

CREATE TABLE IF NOT EXISTS i001 (
    id                              BIGSERIAL PRIMARY KEY,
    product_id                      BIGINT NOT NULL,
    effective_date                  DATE NOT NULL,
    status                          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    remarks                         VARCHAR(255),
    rejection_reason                VARCHAR(255),
    created_by                      BIGINT,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by                     BIGINT,
    updated_at                      TIMESTAMP,
    reviewed_by                     BIGINT,
    reviewed_at                     TIMESTAMP,
    CONSTRAINT fk_i001_product_id FOREIGN KEY (product_id) REFERENCES p001 (id),
    CONSTRAINT fk_i001_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_i001_modified_by FOREIGN KEY (modified_by) REFERENCES u001 (id),
    CONSTRAINT fk_i001_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES u001 (id),
    CONSTRAINT ck_i001_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
COMMENT ON TABLE i001 IS 'Interest rate sheet of a loan product, effective from effective_date. PENDING -> APPROVED/REJECTED (maker-checker). reviewed_by/reviewed_at = the checker who approved or rejected it.';

-- One live (not rejected) sheet per product per effective date.
CREATE UNIQUE INDEX IF NOT EXISTS uq_i001_product_effective_date
    ON i001 (product_id, effective_date) WHERE status <> 'REJECTED';

-- Rate lookup: latest APPROVED sheet of a product on or before a date.
CREATE INDEX IF NOT EXISTS idx_i001_product_status_date
    ON i001 (product_id, status, effective_date DESC);
CREATE INDEX IF NOT EXISTS idx_i001_created_by ON i001 (created_by);
CREATE INDEX IF NOT EXISTS idx_i001_modified_by ON i001 (modified_by);
CREATE INDEX IF NOT EXISTS idx_i001_reviewed_by ON i001 (reviewed_by);

CREATE TABLE IF NOT EXISTS i002 (
    id                              BIGSERIAL PRIMARY KEY,
    rate_master_id                  BIGINT NOT NULL,
    sr_no                           SMALLINT NOT NULL,
    up_to_amount                    NUMERIC(15,2) NOT NULL,
    up_to_months                    INT NOT NULL,
    normal_rate                     NUMERIC(6,2) NOT NULL,
    penal_rate                      NUMERIC(6,2),
    adhoc_rate                      NUMERIC(6,2),
    CONSTRAINT fk_i002_rate_master_id FOREIGN KEY (rate_master_id) REFERENCES i001 (id) ON DELETE CASCADE,
    CONSTRAINT uq_i002_master_sr_no UNIQUE (rate_master_id, sr_no),
    CONSTRAINT uq_i002_master_band UNIQUE (rate_master_id, up_to_amount, up_to_months),
    CONSTRAINT ck_i002_amount CHECK (up_to_amount > 0),
    CONSTRAINT ck_i002_months CHECK (up_to_months BETWEEN 1 AND 480),
    CONSTRAINT ck_i002_normal_rate CHECK (normal_rate >= 0 AND normal_rate <= 100),
    CONSTRAINT ck_i002_penal_rate CHECK (penal_rate IS NULL OR (penal_rate >= 0 AND penal_rate <= 100)),
    CONSTRAINT ck_i002_adhoc_rate CHECK (adhoc_rate IS NULL OR (adhoc_rate >= 0 AND adhoc_rate <= 100))
);
COMMENT ON TABLE i002 IS 'Rate tiers of an interest rate sheet. A loan of amount A and tenure T uses the smallest tier that covers both: among tiers with up_to_amount >= A and up_to_months >= T, the one with the lowest up_to_amount, then the lowest up_to_months. Works for a grid (amount bands x tenure bands) and for a single ladder. The same (up_to_amount, up_to_months) pair cannot repeat.';
COMMENT ON COLUMN i002.penal_rate IS 'Penal rate % for this tier. When empty, the product penalty (p003.penalty_percent) applies.';
COMMENT ON COLUMN i002.adhoc_rate IS 'Adhoc rate %. Stored and returned as entered; business meaning still to be confirmed.';

-- No separate index on i002.rate_master_id: uq_i002_master_sr_no starts with it.

-- ---------------------------------------------------------------------
-- v_interest_rate_master : one row per tier with product details, for
-- reports and the list screen.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_interest_rate_master AS
SELECT
    i001.id AS rate_master_id,
    p001.id AS product_id,
    p001.code AS product_code,
    p001.name AS product_name,
    i001.effective_date,
    i001.status,
    i001.remarks,
    i001.rejection_reason,
    i001.created_by,
    i001.created_at,
    i001.modified_by,
    i001.updated_at,
    i001.reviewed_by,
    i001.reviewed_at,
    i002.id AS tier_id,
    i002.sr_no,
    i002.up_to_amount,
    i002.up_to_months,
    i002.normal_rate,
    i002.penal_rate,
    i002.adhoc_rate
FROM i001
JOIN p001 ON p001.id = i001.product_id
LEFT JOIN i002 ON i002.rate_master_id = i001.id;

-- =============================================================================
-- End of Interest Rate Master
-- =============================================================================

-- ============================================================================
-- V2: Create Bank Accounts (s001) and Approval & Exposure Limits (s002)
-- Allianza LOS - Bank Master Module Database Migration
-- ============================================================================

-- ============================================================================
-- 1. Table s001: Bank Accounts (Logical: bank_accounts)
-- ============================================================================
CREATE TABLE IF NOT EXISTS s001 (
    id                              BIGSERIAL PRIMARY KEY,
    bank_id                         BIGINT,
    bank_name                       VARCHAR(150) NOT NULL,
    branch_id                       BIGINT,
    particulars                     VARCHAR(150),
    particulars_lookup_id           BIGINT,
    account_type                    VARCHAR(50) NOT NULL,
    account_type_lookup_id          BIGINT,
    account_number                  VARCHAR(100) NOT NULL,
    micr_code                       VARCHAR(30),
    ifsc_code                       VARCHAR(30) NOT NULL,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    status                          VARCHAR(30) DEFAULT 'ACTIVE',
    created_by                      BIGINT,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by                     BIGINT,
    updated_at                      TIMESTAMP,
    CONSTRAINT fk_s001_branch_id FOREIGN KEY (branch_id) REFERENCES b001 (id) ON DELETE SET NULL,
    CONSTRAINT fk_s001_particulars_lookup FOREIGN KEY (particulars_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_s001_account_type_lookup FOREIGN KEY (account_type_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_s001_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_s001_modified_by FOREIGN KEY (modified_by) REFERENCES u001 (id)
);

-- Indexes for s001
CREATE INDEX IF NOT EXISTS idx_s001_branch_id ON s001 (branch_id);
CREATE INDEX IF NOT EXISTS idx_s001_account_number ON s001 (account_number);
CREATE INDEX IF NOT EXISTS idx_s001_ifsc_code ON s001 (ifsc_code);
CREATE INDEX IF NOT EXISTS idx_s001_is_active ON s001 (is_active);

-- Prevent duplicate active bank accounts for same branch and account number
CREATE UNIQUE INDEX IF NOT EXISTS uq_s001_active_account ON s001 (COALESCE(branch_id, 0), account_number, ifsc_code) WHERE is_active = TRUE;

-- Compatibility View for Logical Name
CREATE OR REPLACE VIEW bank_accounts AS SELECT * FROM s001;


-- ============================================================================
-- 2. Table s002: Approval and Exposure Limits (Logical: approval_exposure_limits)
-- ============================================================================
CREATE TABLE IF NOT EXISTS s002 (
    id                              BIGSERIAL PRIMARY KEY,
    bank_id                         BIGINT,
    bank_name                       VARCHAR(150),
    branch_id                       BIGINT,
    approval_authority              VARCHAR(150) NOT NULL,
    approval_authority_lookup_id    BIGINT,
    single_party_limit              NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    group_exposure_limit            NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    sanction_margin                 NUMERIC(8,4) NOT NULL DEFAULT 0.0000,
    penalty_charge                  NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    penalty_rate                    NUMERIC(8,4) NOT NULL DEFAULT 0.0000,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    status                          VARCHAR(30) DEFAULT 'ACTIVE',
    created_by                      BIGINT,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by                     BIGINT,
    updated_at                      TIMESTAMP,
    CONSTRAINT fk_s002_branch_id FOREIGN KEY (branch_id) REFERENCES b001 (id) ON DELETE SET NULL,
    CONSTRAINT fk_s002_authority_lookup FOREIGN KEY (approval_authority_lookup_id) REFERENCES m002 (id),
    CONSTRAINT fk_s002_created_by FOREIGN KEY (created_by) REFERENCES u001 (id),
    CONSTRAINT fk_s002_modified_by FOREIGN KEY (modified_by) REFERENCES u001 (id),
    CONSTRAINT chk_s002_single_party_limit CHECK (single_party_limit >= 0),
    CONSTRAINT chk_s002_group_exposure_limit CHECK (group_exposure_limit >= 0),
    CONSTRAINT chk_s002_sanction_margin CHECK (sanction_margin >= 0),
    CONSTRAINT chk_s002_penalty_charge CHECK (penalty_charge >= 0),
    CONSTRAINT chk_s002_penalty_rate CHECK (penalty_rate >= 0)
);

-- Indexes for s002
CREATE INDEX IF NOT EXISTS idx_s002_branch_id ON s002 (branch_id);
CREATE INDEX IF NOT EXISTS idx_s002_approval_authority ON s002 (approval_authority);
CREATE INDEX IF NOT EXISTS idx_s002_authority_lookup_id ON s002 (approval_authority_lookup_id);
CREATE INDEX IF NOT EXISTS idx_s002_is_active ON s002 (is_active);

-- Prevent duplicate active limits for same approval authority per branch
CREATE UNIQUE INDEX IF NOT EXISTS uq_s002_active_authority ON s002 (COALESCE(branch_id, 0), LOWER(TRIM(approval_authority))) WHERE is_active = TRUE;

-- Compatibility View for Logical Name
CREATE OR REPLACE VIEW approval_exposure_limits AS SELECT * FROM s002;
