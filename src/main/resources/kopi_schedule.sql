-- Create database and use it
CREATE DATABASE [Kopi];
GO
USE [Kopi];
GO

----------------------------------------------------------------
-- A) CREATE TABLES (columns only)
----------------------------------------------------------------

-- ROLES
CREATE TABLE dbo.roles (
    role_id     INT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(50)   NOT NULL,
    description NVARCHAR(255)  NULL
);

-- USERS
CREATE TABLE dbo.users (
    user_id           INT IDENTITY(1,1) NOT NULL,
    username          NVARCHAR(100)  NOT NULL,
    email             NVARCHAR(255)  NOT NULL,
    phone             NVARCHAR(20)   NULL,
    password_hash     NVARCHAR(255)  NOT NULL,
    full_name         NVARCHAR(150)  NOT NULL,
    role_id           INT             NOT NULL,
    status            NVARCHAR(20)    NOT NULL,
    email_verified    BIT            NOT NULL,
    avatar_url        NVARCHAR(255)   NULL,
    created_at        DATETIME2(3)    NOT NULL,
    updated_at        DATETIME2(3)    NOT NULL,
    last_login_at     DATETIME2(3)    NULL,
    position_id       INT             NULL
);
GO

-- ADDRESSES
CREATE TABLE dbo.addresses (
    address_id      INT IDENTITY(1,1) NOT NULL,
    address_line    NVARCHAR(255)  NOT NULL,
    ward            NVARCHAR(100)  NULL,
    district        NVARCHAR(100)  NULL,
    city            NVARCHAR(100)  NULL,
    latitude        DECIMAL(10,6)  NULL,
    longitude       DECIMAL(10,6)  NULL,
    created_at      DATETIME2(3)   NOT NULL
);

-- USER_ADDRESSES
CREATE TABLE dbo.user_addresses (
    user_address_id INT IDENTITY(1,1) NOT NULL,
    user_id         INT        NOT NULL,
    address_id      INT        NOT NULL,
    is_default      BIT           NOT NULL,
    created_at      DATETIME2(3)  NOT NULL
);

-- USER_OTPS
CREATE TABLE dbo.user_otps (
    id          INT IDENTITY(1,1) NOT NULL,
    user_id     INT NOT NULL,
    otp_hash    NVARCHAR(255) NOT NULL,
    expires_at  DATETIME2(3) NOT NULL,
    created_at  DATETIME2(3) NOT NULL
);

-- CATEGORIES
CREATE TABLE dbo.categories (
    category_id   INT IDENTITY(1,1) NOT NULL,
    name          NVARCHAR(100) NOT NULL,
    is_active     BIT           NOT NULL,
    display_order INT           NOT NULL
);

-- PRODUCTS
CREATE TABLE dbo.products (
    product_id   INT IDENTITY(1,1) NOT NULL,
    category_id  INT        NOT NULL,
    name         NVARCHAR(150) NOT NULL,
    img_url      NVARCHAR(255) NULL,
    description  NVARCHAR(MAX) NULL,
    sku          NVARCHAR(50)  NULL,
    price        DECIMAL(18,2) NOT NULL,
    is_available BIT           NOT NULL,
    stock_qty    INT           NOT NULL,
    created_at   DATETIME2(3)  NOT NULL,
    updated_at   DATETIME2(3)  NOT NULL
);

-- ORDERS
CREATE TABLE dbo.orders (
    order_id           INT IDENTITY(1,1) NOT NULL,
    order_code         NVARCHAR(30)  NOT NULL,
    customer_id        INT        NULL,
    address_id         INT        NULL,
    table_id           INT        NULL,
    created_by_user_id INT        NULL,
    shipper_user_id    INT        NULL,
    status             NVARCHAR(20)  NOT NULL,
    close_reason       NVARCHAR(500) NULL,
    subtotal_amount    DECIMAL(18,2) NOT NULL,
    discount_amount    DECIMAL(18,2) NOT NULL,
    shipping_amount    DECIMAL(18,2) NOT NULL,
    total_amount       AS (CONVERT(DECIMAL(18,2), subtotal_amount - discount_amount + shipping_amount)) PERSISTED,
    note               NVARCHAR(500) NULL,
    created_at         DATETIME2(3)  NOT NULL,
    updated_at         DATETIME2(3)  NOT NULL,
    closed_at          DATETIME2(3)  NULL
);

-- ORDER_DETAILS
CREATE TABLE dbo.order_details (
    order_detail_id       INT IDENTITY(1,1) NOT NULL,
    order_id              INT        NOT NULL,
    product_id            INT        NOT NULL,
    product_name_snapshot NVARCHAR(150) NOT NULL,
    unit_price            DECIMAL(18,2) NOT NULL,
    quantity              INT           NOT NULL,
    line_total            AS (CONVERT(DECIMAL(18,2), unit_price * quantity)) PERSISTED,
    note                  NVARCHAR(255) NULL
);

-- PAYMENTS
CREATE TABLE dbo.payments (
    payment_id   INT IDENTITY(1,1) NOT NULL,
    order_id     INT        NOT NULL,
    amount       DECIMAL(18,2) NOT NULL,
    method       NVARCHAR(20)  NOT NULL,
    status       NVARCHAR(20)  NOT NULL,
    txn_ref      NVARCHAR(100) NULL,
    paid_at      DATETIME2(3)  NULL,
    created_at   DATETIME2(3)  NOT NULL
);

-- TABLES
CREATE TABLE dbo.tables (
    table_id INT IDENTITY(1,1) NOT NULL,
    number INT NOT NULL,
    name NVARCHAR(100) NULL,
    status NVARCHAR(20) NOT NULL,
    qr_token NVARCHAR(64) NOT NULL,
    created_at DATETIME2(3) NOT NULL,
    updated_at DATETIME2(3) NOT NULL
);

 -- Removed: legacy dbo.schedules (superseded by work_schedules/shift/employee_shifts)

-- INVENTORY_ITEMS
CREATE TABLE dbo.inventory_items (
    inventory_item_id INT IDENTITY(1,1) NOT NULL,
    name              NVARCHAR(150) NOT NULL,
    unit              NVARCHAR(20)  NOT NULL,
    quantity_on_hand  DECIMAL(18,3) NOT NULL,
    reorder_level     DECIMAL(18,3) NOT NULL,
    is_active         BIT           NOT NULL,
    notes             NVARCHAR(255) NULL,
    created_at        DATETIME2(3)  NOT NULL,
    updated_at        DATETIME2(3)  NOT NULL
);

-- INVENTORY_LOG
CREATE TABLE dbo.inventory_log (
    inventory_log_id   INT IDENTITY(1,1) NOT NULL,
    inventory_item_id  INT        NOT NULL,
    change_type        NVARCHAR(10)  NOT NULL,
    quantity_change    DECIMAL(18,3) NOT NULL,
    reason             NVARCHAR(255) NULL,
    order_id           INT        NULL,
    created_by_user_id INT        NULL,
    created_at         DATETIME2(3)  NOT NULL
);

-- DISCOUNT_EVENTS
CREATE TABLE dbo.discount_events (
    discount_event_id INT IDENTITY(1,1) NOT NULL,
    name              NVARCHAR(150) NOT NULL,
    description       NVARCHAR(255) NULL,
    discount_type     NVARCHAR(10)  NOT NULL,
    discount_value    DECIMAL(9,2)  NOT NULL,
    starts_at         DATETIME2(3)  NOT NULL,
    ends_at           DATETIME2(3)  NOT NULL,
    is_active         BIT           NOT NULL,
    created_at        DATETIME2(3)  NOT NULL
);

-- DISCOUNT_EVENT_PRODUCTS
CREATE TABLE dbo.discount_event_products (
    discount_event_product_id INT IDENTITY(1,1) NOT NULL,
    discount_event_id         INT NOT NULL,
    product_id                INT NOT NULL
);

-- DISCOUNT_CODES
CREATE TABLE dbo.discount_codes (
    discount_code_id INT IDENTITY(1,1) NOT NULL,
    code             NVARCHAR(50)   NOT NULL,
    description      NVARCHAR(255)  NULL,
    discount_type    NVARCHAR(10)   NOT NULL,
    discount_value   DECIMAL(9,2)   NOT NULL,
    min_order_amount DECIMAL(18,2)  NULL,
    starts_at        DATETIME2(3)   NOT NULL,
    ends_at          DATETIME2(3)   NOT NULL,
    total_usage_limit INT           NULL,
    per_user_limit    INT           NULL,
    is_active        BIT            NOT NULL,
    usage_count       INT            NOT NULL,
    created_at       DATETIME2(3)   NOT NULL
);

-- DISCOUNT_CODE_REDEMPTIONS
CREATE TABLE dbo.discount_code_redemptions (
    discount_code_redemption_id INT IDENTITY(1,1) NOT NULL,
    discount_code_id            INT NOT NULL,
    order_id                    INT NOT NULL,
    user_id                     INT NULL,
    redeemed_at                 DATETIME2(3) NOT NULL
);

-- POSITIONS (scheduling extension)
CREATE TABLE dbo.positions (
    position_id         INT IDENTITY(1,1) NOT NULL,
    position_name       NVARCHAR(100) NOT NULL,
    description         NVARCHAR(255) NULL,
    is_active           BIT NOT NULL,
    created_by_user_id  INT NULL,
    created_at          DATETIME2(3) NOT NULL,
    updated_by_user_id  INT NULL,
    updated_at          DATETIME2(3) NULL
);

-- SHIFT (patterns)
CREATE TABLE dbo.shift (
    shift_id           INT IDENTITY(1,1) NOT NULL,
    shift_name         NVARCHAR(100) NOT NULL,
    start_time         TIME(0) NOT NULL,
    end_time           TIME(0) NOT NULL,
    description        NVARCHAR(255) NULL,
    is_active          BIT NOT NULL,
    created_by_user_id INT NULL,
    created_at         DATETIME2(3) NOT NULL,
    updated_by_user_id INT NULL,
    updated_at         DATETIME2(3) NULL
);

-- RECURRENCE_PATTERNS
CREATE TABLE dbo.recurrence_patterns (
    recurrence_id   INT IDENTITY(1,1) NOT NULL,
    recurrence_type NVARCHAR(20) NOT NULL,
    day_of_week     NVARCHAR(10) NULL,
    interval_days   INT NULL
);

-- WORK_SCHEDULES
CREATE TABLE dbo.work_schedules (
    work_schedule_id   INT IDENTITY(1,1) NOT NULL,
    name               NVARCHAR(150) NOT NULL,
    description        NVARCHAR(255) NULL,
    start_date         DATE NOT NULL,
    end_date           DATE NOT NULL,
    recurrence_id      INT NULL,
    created_by_user_id INT NULL,
    created_at         DATETIME2(3) NOT NULL,
    updated_by_user_id INT NULL,
    updated_at         DATETIME2(3) NULL
);

-- EMPLOYEE_SHIFTS
CREATE TABLE dbo.employee_shifts (
    employee_shift_id  INT IDENTITY(1,1) NOT NULL,
    work_schedule_id   INT NULL,
    employee_id        INT NULL,
    shift_id           INT NOT NULL,
    shift_date         DATE NOT NULL,
    status             NVARCHAR(20) NOT NULL,
    notes              NVARCHAR(255) NULL,
    override_start_time TIME(0) NULL,
    override_end_time   TIME(0) NULL,
    actual_check_in    DATETIME2(3) NULL,
    actual_check_out   DATETIME2(3) NULL,
    overtime_minutes   INT NULL,
    reason             NVARCHAR(255) NULL,
    created_by_user_id INT NULL,
    created_at         DATETIME2(3) NOT NULL,
    updated_by_user_id INT NULL,
    updated_at         DATETIME2(3) NULL
);

----------------------------------------------------------------
-- B) ADD PRIMARY KEYS
----------------------------------------------------------------

ALTER TABLE dbo.roles            ADD CONSTRAINT PK_roles PRIMARY KEY (role_id);
ALTER TABLE dbo.users            ADD CONSTRAINT PK_users PRIMARY KEY (user_id);
ALTER TABLE dbo.addresses        ADD CONSTRAINT PK_addresses PRIMARY KEY (address_id);
ALTER TABLE dbo.user_addresses   ADD CONSTRAINT PK_user_addresses PRIMARY KEY (user_address_id);
ALTER TABLE dbo.user_otps       ADD CONSTRAINT PK_user_otps PRIMARY KEY (id);
ALTER TABLE dbo.categories       ADD CONSTRAINT PK_categories PRIMARY KEY (category_id);
ALTER TABLE dbo.products         ADD CONSTRAINT PK_products PRIMARY KEY (product_id);
ALTER TABLE dbo.orders           ADD CONSTRAINT PK_orders PRIMARY KEY (order_id);
ALTER TABLE dbo.order_details    ADD CONSTRAINT PK_order_details PRIMARY KEY (order_detail_id);
ALTER TABLE dbo.payments         ADD CONSTRAINT PK_payments PRIMARY KEY (payment_id);
ALTER TABLE dbo.tables           ADD CONSTRAINT PK_tables PRIMARY KEY (table_id);
 -- Removed PK for dbo.schedules
ALTER TABLE dbo.inventory_items  ADD CONSTRAINT PK_inventory_items PRIMARY KEY (inventory_item_id);
ALTER TABLE dbo.inventory_log    ADD CONSTRAINT PK_inventory_log PRIMARY KEY (inventory_log_id);
ALTER TABLE dbo.discount_events          ADD CONSTRAINT PK_discount_events PRIMARY KEY (discount_event_id);
ALTER TABLE dbo.discount_event_products  ADD CONSTRAINT PK_discount_event_products PRIMARY KEY (discount_event_product_id);
ALTER TABLE dbo.discount_codes           ADD CONSTRAINT PK_discount_codes PRIMARY KEY (discount_code_id);
ALTER TABLE dbo.discount_code_redemptions ADD CONSTRAINT PK_discount_code_redemptions PRIMARY KEY (discount_code_redemption_id);
ALTER TABLE dbo.positions         ADD CONSTRAINT PK_positions PRIMARY KEY (position_id);
ALTER TABLE dbo.shift             ADD CONSTRAINT PK_shift PRIMARY KEY (shift_id);
ALTER TABLE dbo.recurrence_patterns ADD CONSTRAINT PK_recurrence_patterns PRIMARY KEY (recurrence_id);
ALTER TABLE dbo.work_schedules    ADD CONSTRAINT PK_work_schedules PRIMARY KEY (work_schedule_id);
ALTER TABLE dbo.employee_shifts   ADD CONSTRAINT PK_employee_shifts PRIMARY KEY (employee_shift_id);

----------------------------------------------------------------
-- C) ADD FOREIGN KEYS
-- (FKs placed after PKs so references exist)
----------------------------------------------------------------

ALTER TABLE dbo.users
    ADD CONSTRAINT FK_users_role FOREIGN KEY (role_id) REFERENCES dbo.roles(role_id);

ALTER TABLE dbo.products
    ADD CONSTRAINT FK_products_category FOREIGN KEY (category_id) REFERENCES dbo.categories(category_id);

ALTER TABLE dbo.orders
    ADD CONSTRAINT FK_orders_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(user_id),
        CONSTRAINT FK_orders_address FOREIGN KEY (address_id) REFERENCES dbo.addresses(address_id),
        CONSTRAINT FK_orders_created_by FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(user_id),
        CONSTRAINT FK_orders_shipper_user FOREIGN KEY (shipper_user_id) REFERENCES dbo.users(user_id);

ALTER TABLE dbo.orders
    ADD CONSTRAINT FK_orders_tables_table_id FOREIGN KEY (table_id) REFERENCES dbo.tables(table_id);

ALTER TABLE dbo.order_details
    ADD CONSTRAINT FK_order_details_order FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id) ON DELETE CASCADE,
        CONSTRAINT FK_order_details_product FOREIGN KEY (product_id) REFERENCES dbo.products(product_id);

ALTER TABLE dbo.payments
    ADD CONSTRAINT FK_payments_order FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id) ON DELETE CASCADE;

 -- Removed FK for dbo.schedules

ALTER TABLE dbo.inventory_log
    ADD CONSTRAINT FK_inventory_log_item FOREIGN KEY (inventory_item_id) REFERENCES dbo.inventory_items(inventory_item_id),
        CONSTRAINT FK_inventory_log_order FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id) ON DELETE SET NULL,
        CONSTRAINT FK_inventory_log_user FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(user_id) ON DELETE NO ACTION;

ALTER TABLE dbo.user_addresses
    ADD CONSTRAINT FK_user_addresses_user FOREIGN KEY (user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE,
        CONSTRAINT FK_user_addresses_address FOREIGN KEY (address_id) REFERENCES dbo.addresses(address_id) ON DELETE CASCADE;

ALTER TABLE dbo.user_otps
    ADD CONSTRAINT FK_user_otps_user FOREIGN KEY (user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE;

ALTER TABLE dbo.discount_event_products
    ADD CONSTRAINT FK_discount_event_products_event FOREIGN KEY (discount_event_id) REFERENCES dbo.discount_events(discount_event_id) ON DELETE CASCADE,
        CONSTRAINT FK_discount_event_products_product FOREIGN KEY (product_id) REFERENCES dbo.products(product_id) ON DELETE CASCADE;

ALTER TABLE dbo.discount_code_redemptions
    ADD CONSTRAINT FK_discount_code_redemptions_code FOREIGN KEY (discount_code_id) REFERENCES dbo.discount_codes(discount_code_id) ON DELETE CASCADE,
        CONSTRAINT FK_discount_code_redemptions_order FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id) ON DELETE CASCADE,
        CONSTRAINT FK_discount_code_redemptions_user FOREIGN KEY (user_id) REFERENCES dbo.users(user_id) ON DELETE SET NULL;

-- Scheduling extension FKs
ALTER TABLE dbo.positions
    ADD CONSTRAINT FK_positions_created_by FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(user_id) ON DELETE NO ACTION;
ALTER TABLE dbo.positions
    ADD CONSTRAINT FK_positions_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES dbo.users(user_id) ON DELETE NO ACTION;

-- Add FK to positions
ALTER TABLE dbo.users
    ADD CONSTRAINT FK_users_position FOREIGN KEY (position_id) REFERENCES dbo.positions(position_id) ON DELETE SET NULL;

ALTER TABLE dbo.shift
    ADD CONSTRAINT FK_shift_created_by FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(user_id) ON DELETE NO ACTION;
ALTER TABLE dbo.shift
    ADD CONSTRAINT FK_shift_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES dbo.users(user_id) ON DELETE NO ACTION;

--ALTER TABLE dbo.recurrence_patterns
    -- no FK

ALTER TABLE dbo.work_schedules
    ADD CONSTRAINT FK_work_schedules_recurrence FOREIGN KEY (recurrence_id) REFERENCES dbo.recurrence_patterns(recurrence_id) ON DELETE SET NULL;
ALTER TABLE dbo.work_schedules
    ADD CONSTRAINT FK_work_schedules_created_by FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(user_id) ON DELETE NO ACTION;
ALTER TABLE dbo.work_schedules
    ADD CONSTRAINT FK_work_schedules_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES dbo.users(user_id) ON DELETE NO ACTION;

ALTER TABLE dbo.employee_shifts
    ADD CONSTRAINT FK_employee_shifts_schedule FOREIGN KEY (work_schedule_id) REFERENCES dbo.work_schedules(work_schedule_id) ON DELETE SET NULL;
ALTER TABLE dbo.employee_shifts
    ADD CONSTRAINT FK_employee_shifts_employee FOREIGN KEY (employee_id) REFERENCES dbo.users(user_id) ON DELETE SET NULL;
ALTER TABLE dbo.employee_shifts
    ADD CONSTRAINT FK_employee_shifts_shift FOREIGN KEY (shift_id) REFERENCES dbo.shift(shift_id) ON DELETE NO ACTION;
ALTER TABLE dbo.employee_shifts
    ADD CONSTRAINT FK_employee_shifts_created_by FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(user_id) ON DELETE NO ACTION;
ALTER TABLE dbo.employee_shifts
    ADD CONSTRAINT FK_employee_shifts_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES dbo.users(user_id) ON DELETE NO ACTION;

----------------------------------------------------------------
-- D) ADD CHECK CONSTRAINTS
----------------------------------------------------------------

ALTER TABLE dbo.users
    ADD CONSTRAINT CK_users_status CHECK (status IN (N'active', N'banned', N'inactive'));

ALTER TABLE dbo.users
    ADD CONSTRAINT CK_users_phone_not_blank CHECK (phone IS NULL OR phone <> '');

ALTER TABLE dbo.order_details
    ADD CONSTRAINT CK_order_details_qty CHECK (quantity > 0);

ALTER TABLE dbo.payments
    ADD CONSTRAINT CK_payments_method CHECK (method IN (N'cash', N'banking')),
        CONSTRAINT CK_payments_status CHECK (status IN (N'pending', N'paid', N'cancelled'));

 -- Removed CHECK for dbo.schedules

ALTER TABLE dbo.orders
    ADD CONSTRAINT CK_orders_shipping_amount CHECK (shipping_amount >= 0);

ALTER TABLE dbo.inventory_log
    ADD CONSTRAINT CK_inventory_log_type CHECK (change_type IN (N'IN', N'OUT', N'ADJUST')),
        CONSTRAINT CK_inventory_log_qty CHECK (quantity_change <> 0);

ALTER TABLE dbo.discount_events
    ADD CONSTRAINT CK_discount_events_type CHECK (discount_type IN (N'PERCENT', N'AMOUNT')),
        CONSTRAINT CK_discount_events_value CHECK (discount_value > 0),
        CONSTRAINT CK_discount_events_time CHECK (starts_at < ends_at),
        CONSTRAINT CK_discount_events_percent_value CHECK (discount_type <> N'PERCENT' OR discount_value <= 100);

ALTER TABLE dbo.discount_codes
    ADD CONSTRAINT CK_discount_codes_type CHECK (discount_type IN (N'PERCENT', N'AMOUNT')),
        CONSTRAINT CK_discount_codes_value CHECK (discount_value > 0),
        CONSTRAINT CK_discount_codes_time CHECK (starts_at < ends_at),
        CONSTRAINT CK_discount_codes_limits CHECK (
        (total_usage_limit IS NULL OR total_usage_limit > 0) AND
        (per_user_limit IS NULL OR per_user_limit > 0)
    );

ALTER TABLE dbo.discount_codes
    ADD CONSTRAINT CK_discount_codes_percent_value CHECK (discount_type <> N'PERCENT' OR discount_value <= 100),
        CONSTRAINT CK_discount_codes_min_amount CHECK (min_order_amount IS NULL OR min_order_amount > 0);

-- Scheduling extension checks
ALTER TABLE dbo.shift
    ADD CONSTRAINT CK_shift_time CHECK (start_time < end_time);

ALTER TABLE dbo.recurrence_patterns
    ADD CONSTRAINT CK_recurrence_type CHECK (recurrence_type IN (N'none', N'daily', N'weekly', N'monthly')),
        CONSTRAINT CK_recurrence_day_of_week CHECK (
        (recurrence_type = N'weekly' AND day_of_week IN (N'Mon',N'Tue',N'Wed',N'Thu',N'Fri',N'Sat',N'Sun'))
        OR (recurrence_type <> N'weekly' AND day_of_week IS NULL)
    ),
        CONSTRAINT CK_recurrence_interval CHECK (
        (recurrence_type = N'none' AND interval_days IS NULL)
        OR (recurrence_type <> N'none' AND interval_days IS NOT NULL AND interval_days > 0)
    );

ALTER TABLE dbo.work_schedules
    ADD CONSTRAINT CK_work_schedules_date CHECK (start_date <= end_date);

ALTER TABLE dbo.employee_shifts
    ADD CONSTRAINT CK_employee_shifts_status CHECK (status IN (N'assigned', N'completed', N'missed', N'cancelled', N'swapped', N'on_leave')),
        CONSTRAINT CK_employee_shifts_override_times CHECK (
        (override_start_time IS NULL AND override_end_time IS NULL)
        OR (override_start_time IS NOT NULL AND override_end_time IS NOT NULL AND override_start_time < override_end_time)
    ),
        CONSTRAINT CK_employee_shifts_actual_times CHECK (
        actual_check_in IS NULL OR actual_check_out IS NULL OR actual_check_in <= actual_check_out
    );

----------------------------------------------------------------
-- E) ADD DEFAULT CONSTRAINTS
----------------------------------------------------------------

ALTER TABLE dbo.users
    ADD CONSTRAINT DF_users_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_users_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at,
        CONSTRAINT DF_users_email_verified DEFAULT (0) FOR email_verified;

ALTER TABLE dbo.user_otps
    ADD CONSTRAINT DF_user_otps_created_at DEFAULT SYSUTCDATETIME() FOR created_at;

ALTER TABLE dbo.addresses
    ADD CONSTRAINT DF_addresses_created_at DEFAULT SYSUTCDATETIME() FOR created_at;

ALTER TABLE dbo.user_addresses
    ADD CONSTRAINT DF_user_addresses_is_default DEFAULT (0) FOR is_default,
        CONSTRAINT DF_user_addresses_created_at DEFAULT SYSUTCDATETIME() FOR created_at;

ALTER TABLE dbo.categories
    ADD CONSTRAINT DF_categories_is_active DEFAULT (1) FOR is_active,
        CONSTRAINT DF_categories_display_order DEFAULT (0) FOR display_order;

ALTER TABLE dbo.products
    ADD CONSTRAINT DF_products_is_available DEFAULT (1) FOR is_available,
        CONSTRAINT DF_products_stock_qty DEFAULT (0) FOR stock_qty,
        CONSTRAINT DF_products_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_products_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;

ALTER TABLE dbo.orders
    ADD CONSTRAINT DF_orders_status DEFAULT (N'pending') FOR status,
        CONSTRAINT DF_orders_subtotal DEFAULT (0) FOR subtotal_amount,
        CONSTRAINT DF_orders_discount DEFAULT (0) FOR discount_amount,
        CONSTRAINT DF_orders_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_orders_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;

ALTER TABLE dbo.orders
    ADD CONSTRAINT DF_orders_shipping DEFAULT (0) FOR shipping_amount;

ALTER TABLE dbo.payments
    ADD CONSTRAINT DF_payments_status DEFAULT (N'pending') FOR status,
        CONSTRAINT DF_payments_created_at DEFAULT SYSUTCDATETIME() FOR created_at;

ALTER TABLE dbo.tables
    ADD CONSTRAINT DF_tables_status DEFAULT (N'AVAILABLE') FOR status,
        CONSTRAINT DF_tables_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_tables_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;

 -- Removed DEFAULT constraints for dbo.schedules

ALTER TABLE dbo.inventory_items
    ADD CONSTRAINT DF_inventory_items_qoh DEFAULT (0) FOR quantity_on_hand,
        CONSTRAINT DF_inventory_items_reorder DEFAULT (0) FOR reorder_level,
        CONSTRAINT DF_inventory_items_is_active DEFAULT (1) FOR is_active,
        CONSTRAINT DF_inventory_items_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_inventory_items_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;

ALTER TABLE dbo.inventory_log
    ADD CONSTRAINT DF_inventory_log_created_at DEFAULT SYSUTCDATETIME() FOR created_at;

ALTER TABLE dbo.discount_events
    ADD CONSTRAINT DF_discount_events_is_active DEFAULT (1) FOR is_active,
        CONSTRAINT DF_discount_events_created_at DEFAULT SYSUTCDATETIME() FOR created_at;

ALTER TABLE dbo.discount_codes
    ADD CONSTRAINT DF_discount_codes_is_active DEFAULT (1) FOR is_active,
        CONSTRAINT DF_discount_codes_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_discount_codes_usage_count DEFAULT (0) FOR usage_count;

ALTER TABLE dbo.positions
    ADD CONSTRAINT DF_positions_is_active DEFAULT (1) FOR is_active,
        CONSTRAINT DF_positions_created_at DEFAULT SYSUTCDATETIME() FOR created_at;

ALTER TABLE dbo.shift
    ADD CONSTRAINT DF_shift_is_active DEFAULT (1) FOR is_active,
        CONSTRAINT DF_shift_created_at DEFAULT SYSUTCDATETIME() FOR created_at;

ALTER TABLE dbo.work_schedules
    ADD CONSTRAINT DF_work_schedules_created_at DEFAULT SYSUTCDATETIME() FOR created_at;

ALTER TABLE dbo.employee_shifts
    ADD CONSTRAINT DF_employee_shifts_status DEFAULT (N'assigned') FOR status,
        CONSTRAINT DF_employee_shifts_created_at DEFAULT SYSUTCDATETIME() FOR created_at;

----------------------------------------------------------------
-- F) CREATE INDEXES & UNIQUE CONSTRAINTS
----------------------------------------------------------------

-- USERS
CREATE UNIQUE INDEX UX_users_username ON dbo.users(username);
CREATE UNIQUE INDEX UX_users_email ON dbo.users(email);
CREATE UNIQUE INDEX UX_users_phone ON dbo.users(phone) WHERE phone IS NOT NULL;
CREATE INDEX IX_users_role_id ON dbo.users(role_id);
CREATE INDEX IX_users_position_id ON dbo.users(position_id);

-- USER_OTPS
CREATE INDEX IX_user_otps_user ON dbo.user_otps(user_id);
CREATE INDEX IX_user_otps_expiry ON dbo.user_otps(expires_at);

-- ROLES
CREATE UNIQUE INDEX UX_roles_name ON dbo.roles(name);

-- TABLES
CREATE UNIQUE INDEX UX_tables_number ON dbo.tables(number);
CREATE UNIQUE INDEX UX_tables_qr_token ON dbo.tables(qr_token);

-- ADDRESSES <-> USERS
CREATE UNIQUE INDEX UX_user_addresses_user_address ON dbo.user_addresses(user_id, address_id);
CREATE INDEX IX_user_addresses_user ON dbo.user_addresses(user_id);
CREATE INDEX IX_user_addresses_address ON dbo.user_addresses(address_id);
CREATE UNIQUE INDEX UX_user_addresses_default_per_user ON dbo.user_addresses(user_id) WHERE is_default = 1;

-- PRODUCTS
CREATE UNIQUE INDEX UX_products_sku_notnull ON dbo.products(sku) WHERE sku IS NOT NULL;
CREATE INDEX IX_products_category ON dbo.products(category_id);
CREATE INDEX IX_products_available ON dbo.products(is_available);

-- DISCOUNTS
CREATE UNIQUE INDEX UX_discount_codes_code ON dbo.discount_codes(code);
CREATE INDEX IX_discount_codes_time ON dbo.discount_codes(starts_at, ends_at);
CREATE INDEX IX_discount_events_time ON dbo.discount_events(starts_at, ends_at);
CREATE UNIQUE INDEX UX_discount_event_products_pair ON dbo.discount_event_products(discount_event_id, product_id);
CREATE UNIQUE INDEX UX_discount_code_redemptions_order ON dbo.discount_code_redemptions(order_id);

-- ORDERS
CREATE UNIQUE INDEX UX_orders_order_code ON dbo.orders(order_code);
CREATE INDEX IX_orders_status_created_at ON dbo.orders(status, created_at DESC);
CREATE INDEX IX_orders_customer ON dbo.orders(customer_id);
CREATE INDEX IX_orders_created_by ON dbo.orders(created_by_user_id);
CREATE INDEX IX_orders_table_id ON dbo.orders(table_id);
CREATE INDEX IX_orders_shipper_user_id ON dbo.orders(shipper_user_id);

-- ORDER DETAILS
CREATE INDEX IX_order_details_order ON dbo.order_details(order_id);
CREATE INDEX IX_order_details_product ON dbo.order_details(product_id);

-- PAYMENTS
CREATE UNIQUE INDEX UX_payments_txn_ref_notnull ON dbo.payments(txn_ref) WHERE txn_ref IS NOT NULL;
CREATE INDEX IX_payments_order ON dbo.payments(order_id);
CREATE INDEX IX_payments_status ON dbo.payments(status);

 -- Removed indexes for dbo.schedules

-- INVENTORY LOG
CREATE INDEX IX_inventory_log_item_created ON dbo.inventory_log(inventory_item_id, created_at);

-- Positional & shift indexes
CREATE UNIQUE INDEX UQ_positions_name ON dbo.positions(position_name);
CREATE INDEX IX_positions_created_by ON dbo.positions(created_by_user_id);
CREATE INDEX IX_positions_updated_by ON dbo.positions(updated_by_user_id);

CREATE UNIQUE INDEX UQ_shift_name ON dbo.shift(shift_name);
CREATE INDEX IX_shift_is_active ON dbo.shift(is_active);

CREATE INDEX IX_work_schedules_date ON dbo.work_schedules(start_date, end_date);

CREATE INDEX IX_employee_shifts_schedule ON dbo.employee_shifts(work_schedule_id);
CREATE INDEX IX_employee_shifts_employee ON dbo.employee_shifts(employee_id);
CREATE INDEX IX_employee_shifts_shift ON dbo.employee_shifts(shift_id);
CREATE INDEX IX_employee_shifts_date ON dbo.employee_shifts(shift_date);

-- Unique assignment per employee per (date, shift) for assigned shifts only
CREATE UNIQUE INDEX UX_employee_shifts_employee_date_shift
    ON dbo.employee_shifts(employee_id, shift_date, shift_id)
    WHERE employee_id IS NOT NULL;

-- Optional: ensure only one open (unassigned) slot per (date, shift)
CREATE UNIQUE INDEX UX_employee_shifts_open_slot_per_date_shift
    ON dbo.employee_shifts(shift_date, shift_id)
    WHERE employee_id IS NULL;

-- Update descriptions for all products

UPDATE dbo.products SET description =
                            'Almond Coffee offers a delicate balance between bold espresso and creamy almond milk, creating a nutty, smooth, and comforting drink.'
WHERE name = 'Almond Coffee';

UPDATE dbo.products SET description =
                            'Tiramisu Coffee blends espresso with tiramisu cream, delivering a dessert-like coffee experience full of sweetness and aroma.'
WHERE name = 'Tiramisu Coffee';

UPDATE dbo.products SET description =
                            'KOPI Salty Coffee (Light) is a signature Vietnamese-style coffee topped with a gentle layer of salted cream, offering a light and balanced flavor.'
WHERE name = 'KOPI Salty Coffee (Light)';

UPDATE dbo.products SET description =
                            'Hue Salty Coffee (Strong) features rich, bold Vietnamese coffee paired with a strong salted cream foam inspired by Hue''s unique coffee culture.'
WHERE name = 'Hue Salty Coffee (Strong)';

UPDATE dbo.products SET description =
                            'Coconut Coffee combines the strong aroma of Vietnamese coffee with the natural sweetness of coconut milk for a tropical twist.'
WHERE name = 'Coconut Coffee';

UPDATE dbo.products SET description =
                            'Avocado Coffee is a creamy fusion of ripe avocado and espresso, offering a silky texture and refreshing flavor.'
WHERE name = 'Avocado Coffee';

UPDATE dbo.products SET description =
                            'Americano (Hot) is made by diluting espresso with hot water, resulting in a smooth, rich, and aromatic black coffee.'
WHERE name = 'Americano (Hot)';

UPDATE dbo.products SET description =
                            'Americano (Iced) delivers the same deep espresso flavor, served chilled for a crisp and refreshing experience.'
WHERE name = 'Americano (Iced)';

UPDATE dbo.products SET description =
                            'Espresso (Black) (Hot) is the purest form of coffee, offering intense aroma, bold flavor, and a velvety crema.'
WHERE name = 'Espresso (Black) (Hot)';

UPDATE dbo.products SET description =
                            'Espresso (Black) (Iced) provides a sharp and refreshing espresso experience over ice for strong-coffee lovers.'
WHERE name = 'Espresso (Black) (Iced)';

UPDATE dbo.products SET description =
                            'Espresso (Milk) combines espresso and steamed milk for a mellow, balanced, and aromatic cup.'
WHERE name = 'Espresso (Milk)';

UPDATE dbo.products SET description =
                            'Cappuccino features rich espresso, steamed milk, and foamed milk in perfect harmony for a classic Italian taste.'
WHERE name = 'Cappuccino';

UPDATE dbo.products SET description =
                            'Latte is a smooth and creamy coffee made with espresso and steamed milk, topped with a light foam layer.'
WHERE name = 'Latte';

UPDATE dbo.products SET description =
                            'Caramel Macchiato layers espresso, steamed milk, and caramel syrup, offering a sweet and indulgent coffee experience.'
WHERE name = 'Caramel Macchiato';

UPDATE dbo.products SET description =
                            'Milk Coffee (Hot) is traditional Vietnamese coffee mixed with condensed milk, offering a rich, sweet, and full-bodied flavor.'
WHERE name = 'Milk Coffee (Hot)';

UPDATE dbo.products SET description =
                            'Milk Coffee (Iced) combines bold Vietnamese coffee with condensed milk and ice for a smooth, refreshing taste.'
WHERE name = 'Milk Coffee (Iced)';

UPDATE dbo.products SET description =
                            'Black Coffee (Hot) delivers the pure strength of roasted beans with no sugar or milk—simple and authentic.'
WHERE name = 'Black Coffee (Hot)';

UPDATE dbo.products SET description =
                            'Black Coffee (Iced) offers a cold, energizing experience for those who love the pure taste of coffee.'
WHERE name = 'Black Coffee (Iced)';

UPDATE dbo.products SET description =
                            'Vietnamese Latte (Hot) blends espresso and condensed milk, offering a smooth sweetness with deep roasted notes.'
WHERE name = 'Vietnamese Latte (Hot)';

UPDATE dbo.products SET description =
                            'Vietnamese Latte (Iced) is a chilled version of the hot latte, offering rich espresso sweetness over ice.'
WHERE name = 'Vietnamese Latte (Iced)';

UPDATE dbo.products SET description =
                            'Dalgona Coffee features whipped coffee foam over milk, creating a creamy and photogenic Korean-style drink.'
WHERE name = 'Dalgona Coffee';

UPDATE dbo.products SET description =
                            'Egg Coffee combines espresso and a creamy egg yolk foam, creating a rich, sweet, and custard-like flavor unique to Vietnam.'
WHERE name = 'Egg Coffee';

UPDATE dbo.products SET description =
                            'Lime Salt Cold Brew delivers a zesty twist on coffee with cold-brew smoothness, lime freshness, and a hint of salt.'
WHERE name = 'Lime Salt Cold Brew';

UPDATE dbo.products SET description =
                            'Orange Cold Brew mixes the bitterness of cold brew with the sweetness and aroma of fresh orange juice.'
WHERE name = 'Orange Cold Brew';

UPDATE dbo.products SET description =
                            'Pineapple Cold Brew offers a tropical fusion of cold brew coffee and juicy pineapple for a bright, fruity kick.'
WHERE name = 'Pineapple Cold Brew';

UPDATE dbo.products SET description =
                            'Cold Brew Original is slow-brewed for 12 hours to achieve smooth, low-acid coffee with deep flavor.'
WHERE name = 'Cold Brew Original';

UPDATE dbo.products SET description =
                            'Red Bean Milk Tea combines chewy red beans with smooth milk tea for a sweet and textural experience.'
WHERE name = 'Red Bean Milk Tea';

UPDATE dbo.products SET description =
                            'Traditional Milk Tea offers the classic balance of strong tea and creamy milk, loved by all ages.'
WHERE name = 'Traditional Milk Tea';

UPDATE dbo.products SET description =
                            'Cream Cheese and Toasted Coconut Milk Tea is rich and tropical with cheese foam and coconut aroma.'
WHERE name = 'Cream Cheese and Toasted Coconut Milk Tea';

UPDATE dbo.products SET description =
                            'Brown Sugar Bubble Fresh Milk features boba pearls in fresh milk with a sweet brown sugar syrup swirl.'
WHERE name = 'Brown Sugar Bubble Fresh Milk';

UPDATE dbo.products SET description =
                            'Peach Jelly Tea is a fruity, fragrant tea served with chewy peach jelly for extra texture.'
WHERE name = 'Peach Jelly Tea';

UPDATE dbo.products SET description =
                            'Orange Lemongrass Peach Tea combines citrus freshness with peach sweetness and lemongrass aroma.'
WHERE name = 'Orange Lemongrass Peach Tea';

UPDATE dbo.products SET description =
                            'Lychee Lotus Tea offers floral and fruity notes, blending lychee flavor with lotus petals.'
WHERE name = 'Lychee Lotus Tea';

UPDATE dbo.products SET description =
                            'Lotus Tea With Cheese Foam is a fragrant lotus tea topped with rich cheese foam for a creamy contrast.'
WHERE name = 'Lotus Tea With Cheese Foam';

UPDATE dbo.products SET description =
                            'Mango Passion Fruit Iced Tea blends tropical mango and passion fruit with iced tea for a tangy refreshment.'
WHERE name = 'Mango Passion Fruit Iced Tea';

UPDATE dbo.products SET description =
                            'Honey Ginger Tea (Hot) soothes your throat with warm ginger spice and natural honey sweetness.'
WHERE name = 'Honey Ginger Tea (Hot)';

UPDATE dbo.products SET description =
                            'Herbal Tea (Hot) is a calming blend of herbs, offering a relaxing and caffeine-free experience.'
WHERE name = 'Herbal Tea (Hot)';

UPDATE dbo.products SET description =
                            'Detox Celery, Pineapple, Lemon, Honey is a refreshing detox drink rich in vitamins and natural sweetness.'
WHERE name = 'Detox Celery, Pineapple, Lemon, Honey';

UPDATE dbo.products SET description =
                            'Lemon Juice with Honey and Chia Seed is a revitalizing drink full of vitamin C, texture, and freshness.'
WHERE name = 'Lemon Juice with Honey and Chia Seed';

UPDATE dbo.products SET description =
                            'Coconut Juice offers natural sweetness and refreshing hydration straight from the tropics.'
WHERE name = 'Coconut Juice';

UPDATE dbo.products SET description =
                            'Pineapple Juice is a bright, tangy tropical drink full of vitamin C and sweetness.'
WHERE name = 'Pineapple Juice';

UPDATE dbo.products SET description =
                            'Watermelon Juice is a cooling, naturally sweet juice perfect for hot days.'
WHERE name = 'Watermelon Juice';

UPDATE dbo.products SET description =
                            'Passion Fruit Juice delivers a tart and tropical burst of flavor with every sip.'
WHERE name = 'Passion Fruit Juice';

UPDATE dbo.products SET description =
                            'Orange Juice is freshly squeezed with natural sweetness and high vitamin C content.'
WHERE name = 'Orange Juice';

UPDATE dbo.products SET description =
                            'Matcha Jelly is a creamy matcha drink with soft jelly, offering rich tea flavor and fun texture.'
WHERE name = 'Matcha Jelly';

UPDATE dbo.products SET description =
                            'Caramel Jelly combines smooth caramel sweetness with chewy jelly for a playful drink.'
WHERE name = 'Caramel Jelly';

UPDATE dbo.products SET description =
                            'Choco Jelly offers the taste of chocolate milk blended with soft jelly for chocolate lovers.'
WHERE name = 'Choco Jelly';

UPDATE dbo.products SET description =
                            'Chocolate Cookie (Oreo) mixes crushed Oreos with chocolate cream for a rich dessert-like drink.'
WHERE name = 'Chocolate Cookie (Oreo)';

UPDATE dbo.products SET description =
                            'Matcha Cookie (Oreo) blends matcha flavor with Oreo bits for a unique Japanese-inspired treat.'
WHERE name = 'Matcha Cookie (Oreo)';

UPDATE dbo.products SET description =
                            'Mango Yogurt combines smooth yogurt and sweet mango for a creamy, tropical delight.'
WHERE name = 'Mango Yogurt';

UPDATE dbo.products SET description =
                            'Peach Yogurt offers a fruity and refreshing yogurt drink with juicy peach flavor.'
WHERE name = 'Peach Yogurt';

UPDATE dbo.products SET description =
                            'Strawberry Yogurt is made with fresh strawberries and creamy yogurt for a sweet, tangy flavor.'
WHERE name = 'Strawberry Yogurt';

UPDATE dbo.products SET description =
                            'Blueberry Yogurt delivers a burst of berry flavor blended into rich, smooth yogurt.'
WHERE name = 'Blueberry Yogurt';

UPDATE dbo.products SET description =
                            'Coconut Red Bean combines coconut milk and red beans for a traditional, smooth, and lightly sweet drink.'
WHERE name = 'Coconut Red Bean';

UPDATE dbo.products SET description =
                            'Matcha Latte blends premium Japanese matcha with steamed milk for a creamy and earthy taste.'
WHERE name = 'Matcha Latte';

UPDATE dbo.products SET description =
                            'Chocolate drink features rich cocoa flavor and smooth milk for a comforting classic.'
WHERE name = 'Chocolate';

UPDATE dbo.products SET description =
                            'Fresh Milk is simple, creamy, and naturally sweet — perfect alone or with snacks.'
WHERE name = 'Fresh Milk';

UPDATE dbo.products SET description =
                            'Beef Spaghetti features tender beef sauce over al dente pasta, cooked in Italian style.'
WHERE name = 'Beef Spaghetti';

UPDATE dbo.products SET description =
                            'Chicken Spaghetti offers tender chicken in creamy sauce for a light yet satisfying meal.'
WHERE name = 'Chicken Spaghetti';

UPDATE dbo.products SET description =
                            'Egg Toast features golden toast topped with soft egg and melted cheese for a breakfast favorite.'
WHERE name = 'Egg Toast';

UPDATE dbo.products SET description =
                            'Tuna Toast is a savory combination of tuna spread, cheese, and toasted bread.'
WHERE name = 'Tuna Toast';

UPDATE dbo.products SET description =
                            'Ham Toast layers grilled ham and cheese on crispy toast — simple and delicious.'
WHERE name = 'Ham Toast';

UPDATE dbo.products SET description =
                            'Tiramisu dessert offers layers of coffee-soaked sponge and mascarpone cream for a rich Italian flavor.'
WHERE name = 'Tiramisu';

UPDATE dbo.products SET description =
                            'Matcha Mousse is a light and airy dessert blending matcha flavor with smooth creaminess.'
WHERE name = 'Matcha Mousse';

UPDATE dbo.products SET description =
                            'Blueberry Cheese features creamy cheesecake with a tangy blueberry topping.'
WHERE name = 'Blueberry Cheese';

UPDATE dbo.products SET description =
                            'Passion Fruit Cheese combines creamy cheesecake with the tropical tang of passion fruit.'
WHERE name = 'Passion Fruit Cheese';

UPDATE dbo.products SET description =
                            'Coca Cola – the world’s favorite fizzy drink, refreshing and sweet with caramel flavor.'
WHERE name = 'Coca Cola';

UPDATE dbo.products SET description =
                            'Pepsi is a crisp and refreshing cola with a balanced sweet taste.'
WHERE name = 'Pepsi';

UPDATE dbo.products SET description =
                            '7Up is a clear lemon-lime soda that refreshes instantly with a citrus sparkle.'
WHERE name = '7Up';

UPDATE dbo.products SET description =
                            'Evian Water is natural mineral water from the French Alps, pure and refreshing.'
WHERE name = 'Evian Water';

----------------------------------------------------------------
-- END OF SCHEMA
----------------------------------------------------------------

-- NOTE: No seed data inserted (per request).
-- If you later want to migrate existing data from the old dbo.schedules into employee_shifts,
-- do ETL/mapping externally taking into account shift/shift_date/start_time/end_time mapping.
