

-- ==========================================
-- 1. BANK INFORMATION
-- ==========================================

CREATE TABLE IF NOT EXISTS bank_information (
    id BIGSERIAL PRIMARY KEY,

    -- Basic Bank Information
    bank_name VARCHAR(150) NOT NULL,
    bank_code VARCHAR(50) NOT NULL UNIQUE,
    bank_email VARCHAR(150) NOT NULL,
    contact_number VARCHAR(20),
    legal_name VARCHAR(200),
    bank_type VARCHAR(50),

    -- Registration Details
    pan VARCHAR(20),
    gst_no VARCHAR(30),
    license_no VARCHAR(50),
    website VARCHAR(255),
    logo_url VARCHAR(500),

    -- Bank Status
    status VARCHAR(30) DEFAULT 'ACTIVE',
    regulatory_status VARCHAR(30) DEFAULT 'ACTIVE',

    -- Banking Membership
    direct_clg_member BOOLEAN DEFAULT FALSE,
    direct_member_iftas BOOLEAN DEFAULT FALSE,

    -- MICR and IFSC Details
    micr1 VARCHAR(20),
    micr2 VARCHAR(20),
    micr3 VARCHAR(20),
    micr_number VARCHAR(30),
    ifsc_code VARCHAR(20),

    -- Branch Information
    no_of_branches INT,

    -- Sponsor Bank Information
    sponsor_bank_clg VARCHAR(150),
    sponsor_bank_iftas VARCHAR(150),

    -- Registered Office Address
    address_type VARCHAR(50) DEFAULT 'Registered Office',
    unit_gala VARCHAR(150),
    street_road VARCHAR(200),
    landmark VARCHAR(150),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    pincode VARCHAR(20),

    -- Existing Address Field
    address TEXT,

    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);


-- ==========================================
-- 2. DATABASE CONFIGURATION
-- ==========================================

CREATE TABLE IF NOT EXISTS database_configuration (
    id BIGSERIAL PRIMARY KEY,
    bank_id BIGINT NOT NULL UNIQUE,
    database_name VARCHAR(100) NOT NULL UNIQUE,
    database_host VARCHAR(255) NOT NULL,
    database_port INTEGER NOT NULL DEFAULT 5432,
    database_type VARCHAR(50) DEFAULT 'POSTGRESQL',
    is_connected BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_database_bank
        FOREIGN KEY (bank_id)
        REFERENCES bank_information(id)
);


-- ==========================================
-- 3. SYSTEM SETUP
-- ==========================================

CREATE TABLE IF NOT EXISTS system_setup (
    id BIGSERIAL PRIMARY KEY,
    bank_id BIGINT NOT NULL UNIQUE,
    setup_completed BOOLEAN NOT NULL DEFAULT FALSE,
    database_created BOOLEAN NOT NULL DEFAULT FALSE,
    migration_completed BOOLEAN NOT NULL DEFAULT FALSE,
    super_admin_created BOOLEAN NOT NULL DEFAULT FALSE,
    setup_started_at TIMESTAMP,
    setup_completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_setup_bank
        FOREIGN KEY (bank_id)
        REFERENCES bank_information(id)
);


-- ==========================================
-- 4. INITIAL ADMIN SETUP
-- ==========================================

CREATE TABLE IF NOT EXISTS initial_admin_setup (
    id BIGSERIAL PRIMARY KEY,
    bank_id BIGINT NOT NULL UNIQUE,
    admin_name VARCHAR(150) NOT NULL,
    admin_email VARCHAR(150) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT,
    is_created BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_admin_setup_bank
        FOREIGN KEY (bank_id)
        REFERENCES bank_information(id)
);


-- ==========================================
-- 5. SETUP ACTIVITY LOG
-- ==========================================

CREATE TABLE IF NOT EXISTS setup_activity_log (
    id BIGSERIAL PRIMARY KEY,
    bank_id BIGINT,
    activity VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_setup_log_bank
        FOREIGN KEY (bank_id)
        REFERENCES bank_information(id)
);


-- ==========================================
-- 6. VIEW COMPLETE BANK SETUP DETAILS
-- ==========================================

SELECT
    -- Bank Basic Information
    b.id AS bank_id,
    b.bank_name,
    b.bank_code,
    b.bank_email,
    b.contact_number,
    b.legal_name,
    b.bank_type,

    -- Registration Details
    b.pan,
    b.gst_no,
    b.license_no,
    b.website,
    b.logo_url,

    -- Bank Status
    b.status AS bank_status,
    b.regulatory_status,

    -- Banking Membership
    b.direct_clg_member,
    b.direct_member_iftas,

    -- MICR and IFSC
    b.micr1,
    b.micr2,
    b.micr3,
    b.micr_number,
    b.ifsc_code,

    -- Branch Details
    b.no_of_branches,

    -- Sponsor Bank
    b.sponsor_bank_clg,
    b.sponsor_bank_iftas,

    -- Registered Office Address
    b.address_type,
    b.unit_gala,
    b.street_road,
    b.landmark,
    b.city,
    b.state,
    b.country,
    b.pincode,
    b.address,

    -- Database Configuration
    d.database_name,
    d.database_host,
    d.database_port,
    d.database_type,
    d.is_connected,

    -- System Setup
    s.database_created,
    s.migration_completed,
    s.super_admin_created,
    s.setup_completed,
    s.setup_started_at,
    s.setup_completed_at,

    -- Initial Admin
    a.admin_name,
    a.admin_email,
    a.username,
    a.is_created

FROM bank_information b

LEFT JOIN database_configuration d
    ON b.id = d.bank_id

LEFT JOIN system_setup s
    ON b.id = s.bank_id

LEFT JOIN initial_admin_setup a
    ON b.id = a.bank_id;


-- ==========================================
-- END OF SETUP DATABASE SCHEMA
-- ==========================================