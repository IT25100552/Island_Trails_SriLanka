-- ==========================================================
-- Island Trails Lanka (Pvt) Ltd - Database Schema DDL (MSSQL)
-- Project Code: 2026-Y2-S1-KU-08 (SLIIT)
-- ==========================================================



USE IslandTrail;
GO

-- 1. USERS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='users' AND xtype='U')
BEGIN
    CREATE TABLE users (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        email VARCHAR(150) NOT NULL UNIQUE,
        name VARCHAR(100) NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        role VARCHAR(30) NOT NULL, -- CUSTOMER, TOUR_OPS_MANAGER, TRAVEL_CONSULTANT, FINANCE_OFFICER, CUSTOMER_RELATIONS_OFFICER, IT_SYSTEMS_OFFICER
        status VARCHAR(30) NOT NULL, -- ACTIVE, DEACTIVATED, PENDING_EMAIL_VERIFICATION
        failed_login_attempts INT NOT NULL DEFAULT 0,
        locked_until DATETIME NULL,
        last_login_at DATETIME NULL,
        deactivated_at DATETIME NULL,
        phone_number VARCHAR(255) NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 2. USER ACTIONS (AUDIT LOG) TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='user_actions' AND xtype='U')
BEGIN
    CREATE TABLE user_actions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NULL,
        user_email VARCHAR(255) NULL,
        action VARCHAR(50) NOT NULL,
        delta VARCHAR(MAX) NULL,
        timestamp DATETIME NOT NULL DEFAULT GETDATE(),
        ip_address VARCHAR(50) NULL
    );
END
GO

-- 3. SYSTEM CONFIGS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='system_configs' AND xtype='U')
BEGIN
    CREATE TABLE system_configs (
        config_key VARCHAR(100) PRIMARY KEY,
        config_value VARCHAR(255) NOT NULL,
        description VARCHAR(255) NULL,
        updated_by BIGINT NULL,
        updated_at DATETIME NULL
    );
END
GO

-- 4. PACKAGES TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='packages' AND xtype='U')
BEGIN
    CREATE TABLE packages (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(150) NOT NULL,
        destination VARCHAR(100) NOT NULL,
        description VARCHAR(MAX) NULL,
        base_price DECIMAL(12,2) NOT NULL,
        duration_days INT NOT NULL,
        image_data VARBINARY(MAX) NULL,
        image_mime_type VARCHAR(50) NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, INACTIVE
        origin VARCHAR(30) NOT NULL DEFAULT 'BROWSABLE', -- BROWSABLE, FROM_QUOTATION
        created_by BIGINT NULL,
        published_at DATETIME NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 5. REVIEWS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='reviews' AND xtype='U')
BEGIN
    CREATE TABLE reviews (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        booking_id BIGINT NOT NULL,
        customer_id BIGINT NOT NULL,
        customer_name VARCHAR(255) NULL,
        package_id BIGINT NOT NULL,
        rating INT NOT NULL,
        comment VARCHAR(MAX) NULL,
        photo_data VARBINARY(MAX) NULL,
        photo_mime_type VARCHAR(50) NULL,
        responded_at DATETIME NULL,
        staff_response VARCHAR(MAX) NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 6. RESOURCES TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='resources' AND xtype='U')
BEGIN
    CREATE TABLE resources (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        resource_type VARCHAR(30) NOT NULL, -- VEHICLE, GUIDE, HOTEL_ROOM, ACTIVITY
        name VARCHAR(150) NOT NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
        unit_cost DECIMAL(12,2) NOT NULL,
        plate_number VARCHAR(50) NULL,
        seat_capacity INT NULL,
        vehicle_model VARCHAR(100) NULL,
        languages VARCHAR(200) NULL,
        certification VARCHAR(100) NULL,
        contact_number VARCHAR(50) NULL,
        hotel_name VARCHAR(150) NULL,
        room_number VARCHAR(50) NULL,
        bed_capacity INT NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 7. RESOURCE ASSIGNMENTS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='resource_assignments' AND xtype='U')
BEGIN
    CREATE TABLE resource_assignments (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        package_id BIGINT NULL,
        booking_id BIGINT NULL,
        resource_id BIGINT NOT NULL,
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE', -- RESERVED, CONFIRMED, AVAILABLE
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 8. TRIP REQUESTS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='trip_requests' AND xtype='U')
BEGIN
    CREATE TABLE trip_requests (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        customer_id BIGINT NOT NULL,
        customer_name VARCHAR(255) NULL,
        budget DECIMAL(12,2) NOT NULL,
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        interests VARCHAR(255) NULL,
        special_requirements VARCHAR(MAX) NULL,
        number_of_travelers INT NOT NULL DEFAULT 1,
        status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED', -- SUBMITTED, UNDER_REVIEW, QUOTED, ACCEPTED, CONVERTED_TO_BOOKING
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 9. QUOTATIONS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='quotations' AND xtype='U')
BEGIN
    CREATE TABLE quotations (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        trip_request_id BIGINT NOT NULL,
        consultant_id BIGINT NOT NULL,
        consultant_name VARCHAR(255) NULL,
        quotation_version INT NOT NULL DEFAULT 1,
        base_subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
        profit_margin_percent DECIMAL(5,2) NOT NULL DEFAULT 15.00,
        total_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
        status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SENT, APPROVED, REJECTED, SUPERSEDED, CONVERTED
        itinerary_notes VARCHAR(MAX) NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 10. QUOTATION LINES TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='quotation_lines' AND xtype='U')
BEGIN
    CREATE TABLE quotation_lines (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        quotation_id BIGINT NOT NULL,
        resource_id BIGINT NOT NULL,
        resource_name VARCHAR(255) NULL,
        resource_type VARCHAR(30) NULL,
        quantity INT NOT NULL DEFAULT 1,
        unit_cost DECIMAL(12,2) NOT NULL,
        line_cost DECIMAL(12,2) NOT NULL,
        sequence INT NOT NULL DEFAULT 1,
        activity_description VARCHAR(255) NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL,
        CONSTRAINT fk_quotation FOREIGN KEY (quotation_id) REFERENCES quotations(id) ON DELETE CASCADE
    );
END
GO

-- 11. BOOKINGS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='bookings' AND xtype='U')
BEGIN
    CREATE TABLE bookings (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        customer_id BIGINT NOT NULL,
        customer_name VARCHAR(255) NULL,
        package_id BIGINT NOT NULL,
        package_name VARCHAR(255) NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT', -- PENDING_PAYMENT, CONFIRMED, ONGOING, COMPLETED, CANCELLED
        booking_date DATE NOT NULL,
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        total_price DECIMAL(12,2) NOT NULL,
        number_of_travelers INT NOT NULL DEFAULT 1,
        special_requests VARCHAR(MAX) NULL,
        confirmed_at DATETIME NULL,
        cancelled_at DATETIME NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 12. PAYMENTS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='payments' AND xtype='U')
BEGIN
    CREATE TABLE payments (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        booking_id BIGINT NOT NULL UNIQUE,
        customer_id BIGINT NOT NULL,
        amount DECIMAL(12,2) NOT NULL,
        method VARCHAR(30) NOT NULL, -- CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, ONLINE_WALLET
        transaction_id VARCHAR(100) NOT NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, VERIFIED, DECLINED, TIMEOUT, DUPLICATE_FLAGGED
        processed_at DATETIME NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 13. INVOICES TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='invoices' AND xtype='U')
BEGIN
    CREATE TABLE invoices (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        booking_id BIGINT NOT NULL,
        invoice_number VARCHAR(50) NOT NULL UNIQUE,
        issue_date DATE NOT NULL,
        due_date DATE NULL,
        total_amount DECIMAL(12,2) NOT NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'ISSUED', -- ISSUED, PAID, VOID
        customer_name VARCHAR(255) NULL,
        customer_email VARCHAR(255) NULL,
        package_name VARCHAR(255) NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 14. REFUNDS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='refunds' AND xtype='U')
BEGIN
    CREATE TABLE refunds (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        payment_id BIGINT NOT NULL,
        booking_id BIGINT NOT NULL,
        customer_id BIGINT NOT NULL,
        customer_name VARCHAR(255) NULL,
        reason VARCHAR(255) NOT NULL,
        requested_amount DECIMAL(12,2) NOT NULL,
        approved_amount DECIMAL(12,2) NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED', -- REQUESTED, APPROVED, REJECTED
        decided_by BIGINT NULL,
        decided_by_name VARCHAR(255) NULL,
        decision_reason VARCHAR(MAX) NULL,
        refunded_at DATETIME NULL,
        refund_confirmation_code VARCHAR(100) NULL,
        requested_at DATETIME NOT NULL DEFAULT GETDATE(),
        decided_at DATETIME NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 15. TICKETS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='tickets' AND xtype='U')
BEGIN
    CREATE TABLE tickets (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        customer_id BIGINT NOT NULL,
        customer_name VARCHAR(255) NULL,
        customer_email VARCHAR(255) NULL,
        subject VARCHAR(150) NOT NULL,
        description VARCHAR(MAX) NOT NULL,
        category VARCHAR(30) NOT NULL, -- BOOKING, PAYMENT, GUIDE_QUALITY, ACCOMMODATION, TRANSPORT, GENERAL
        priority VARCHAR(30) NOT NULL, -- LOW, MEDIUM, HIGH
        inquiry_type VARCHAR(30) NOT NULL, -- QUESTION, COMPLAINT, REVIEW
        department VARCHAR(30) NOT NULL, -- FINANCE, TOUR_OPS, TOUR_CONSULTANT, GENERAL
        status VARCHAR(30) NOT NULL DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, RESOLVED, CLOSED, SPAM
        assigned_to BIGINT NULL,
        assigned_to_name VARCHAR(255) NULL,
        resolved_at DATETIME NULL,
        closed_at DATETIME NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

-- 16. TICKET REPLIES TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='ticket_replies' AND xtype='U')
BEGIN
    CREATE TABLE ticket_replies (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        ticket_id BIGINT NOT NULL,
        replied_by BIGINT NULL,
        replied_by_name VARCHAR(255) NULL,
        message VARCHAR(MAX) NOT NULL,
        is_staff_reply BIT NOT NULL DEFAULT 0,
        attachments VARCHAR(255) NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NULL
    );
END
GO

PRINT 'Island Trails Lanka database schema created successfully.';
GO
