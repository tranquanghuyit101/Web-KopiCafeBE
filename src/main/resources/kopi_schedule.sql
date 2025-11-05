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
    size_id               INT        NULL,
    product_name_snapshot NVARCHAR(150) NOT NULL,
    unit_price            DECIMAL(18,2) NOT NULL,
    quantity              INT           NOT NULL,
    line_total            AS (CONVERT(DECIMAL(18,2), unit_price * quantity)) PERSISTED,
    note                  NVARCHAR(255) NULL
);

-- SIZES
CREATE TABLE dbo.sizes (
    size_id        INT IDENTITY(1,1) NOT NULL,
    name           NVARCHAR(50)  NOT NULL,
    code           NVARCHAR(20)  NULL,
    display_order  INT           NOT NULL,
    created_at     DATETIME2(3)  NOT NULL,
    updated_at     DATETIME2(3)  NOT NULL
);

-- PRODUCT_SIZES (product <-> size with price per size)
CREATE TABLE dbo.product_sizes (
    product_size_id INT IDENTITY(1,1) NOT NULL,
    product_id      INT           NOT NULL,
    size_id         INT           NOT NULL,
    price           DECIMAL(18,2) NOT NULL,
    is_available    BIT           NOT NULL
);

-- ADD_ONS (toppings)
CREATE TABLE dbo.add_ons (
    add_on_id      INT IDENTITY(1,1) NOT NULL,
    name           NVARCHAR(100) NOT NULL,
    display_order  INT          NOT NULL,
    created_at     DATETIME2(3) NOT NULL,
    updated_at     DATETIME2(3) NOT NULL
);

-- PRODUCT_ADD_ONS (mapping add-on availability and price per product)
CREATE TABLE dbo.product_add_ons (
    product_add_on_id INT IDENTITY(1,1) NOT NULL,
    product_id        INT           NOT NULL,
    add_on_id         INT           NOT NULL,
    price             DECIMAL(18,2) NOT NULL,
    is_available      BIT           NOT NULL
);

-- ORDER_DETAIL_ADD_ONS (selected add-ons per order line)
CREATE TABLE dbo.order_detail_add_ons (
    order_detail_add_on_id INT IDENTITY(1,1) NOT NULL,
    order_detail_id        INT           NOT NULL,
    add_on_id              INT           NOT NULL,
    unit_price_snapshot    DECIMAL(18,2) NOT NULL
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
ALTER TABLE dbo.sizes            ADD CONSTRAINT PK_sizes PRIMARY KEY (size_id);
ALTER TABLE dbo.product_sizes    ADD CONSTRAINT PK_product_sizes PRIMARY KEY (product_size_id);
ALTER TABLE dbo.add_ons          ADD CONSTRAINT PK_add_ons PRIMARY KEY (add_on_id);
ALTER TABLE dbo.product_add_ons  ADD CONSTRAINT PK_product_add_ons PRIMARY KEY (product_add_on_id);
ALTER TABLE dbo.order_detail_add_ons ADD CONSTRAINT PK_order_detail_add_ons PRIMARY KEY (order_detail_add_on_id);
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

ALTER TABLE dbo.order_details
    ADD CONSTRAINT FK_order_details_size FOREIGN KEY (size_id) REFERENCES dbo.sizes(size_id) ON DELETE NO ACTION;

ALTER TABLE dbo.product_sizes
    ADD CONSTRAINT FK_product_sizes_product FOREIGN KEY (product_id) REFERENCES dbo.products(product_id) ON DELETE NO ACTION,
        CONSTRAINT FK_product_sizes_size FOREIGN KEY (size_id) REFERENCES dbo.sizes(size_id) ON DELETE NO ACTION;

ALTER TABLE dbo.product_add_ons
    ADD CONSTRAINT FK_product_add_ons_product FOREIGN KEY (product_id) REFERENCES dbo.products(product_id) ON DELETE NO ACTION,
        CONSTRAINT FK_product_add_ons_addon FOREIGN KEY (add_on_id) REFERENCES dbo.add_ons(add_on_id) ON DELETE NO ACTION;

ALTER TABLE dbo.order_detail_add_ons
    ADD CONSTRAINT FK_order_detail_add_ons_detail FOREIGN KEY (order_detail_id) REFERENCES dbo.order_details(order_detail_id) ON DELETE NO ACTION,
        CONSTRAINT FK_order_detail_add_ons_addon FOREIGN KEY (add_on_id) REFERENCES dbo.add_ons(add_on_id) ON DELETE NO ACTION;

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

-- Sizes defaults
ALTER TABLE dbo.sizes
    ADD CONSTRAINT DF_sizes_display_order DEFAULT (0) FOR display_order,
        CONSTRAINT DF_sizes_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_sizes_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;

-- Add-ons defaults
ALTER TABLE dbo.add_ons
    ADD CONSTRAINT DF_add_ons_display_order DEFAULT (0) FOR display_order,
        CONSTRAINT DF_add_ons_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_add_ons_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;

-- Mapping defaults
ALTER TABLE dbo.product_sizes
    ADD CONSTRAINT DF_product_sizes_is_available DEFAULT (1) FOR is_available;

ALTER TABLE dbo.product_add_ons
    ADD CONSTRAINT DF_product_add_ons_is_available DEFAULT (1) FOR is_available;

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
CREATE INDEX IX_order_details_size ON dbo.order_details(size_id);

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

-- SIZES
CREATE UNIQUE INDEX UX_sizes_name ON dbo.sizes(name);
CREATE UNIQUE INDEX UX_sizes_code_notnull ON dbo.sizes(code) WHERE code IS NOT NULL;

-- PRODUCT_SIZES
CREATE UNIQUE INDEX UX_product_sizes_pair ON dbo.product_sizes(product_id, size_id);
CREATE INDEX IX_product_sizes_product ON dbo.product_sizes(product_id);
CREATE INDEX IX_product_sizes_size ON dbo.product_sizes(size_id);

-- ADD_ONS
CREATE UNIQUE INDEX UX_add_ons_name ON dbo.add_ons(name);

-- PRODUCT_ADD_ONS
CREATE UNIQUE INDEX UX_product_add_ons_pair ON dbo.product_add_ons(product_id, add_on_id);
CREATE INDEX IX_product_add_ons_product ON dbo.product_add_ons(product_id);
CREATE INDEX IX_product_add_ons_add_on ON dbo.product_add_ons(add_on_id);

-- ORDER_DETAIL_ADD_ONS
CREATE INDEX IX_order_detail_add_ons_detail ON dbo.order_detail_add_ons(order_detail_id);
CREATE INDEX IX_order_detail_add_ons_addon ON dbo.order_detail_add_ons(add_on_id);

-- Unique assignment per employee per (date, shift) for assigned shifts only
CREATE UNIQUE INDEX UX_employee_shifts_employee_date_shift
    ON dbo.employee_shifts(employee_id, shift_date, shift_id)
    WHERE employee_id IS NOT NULL;

-- Optional: ensure only one open (unassigned) slot per (date, shift)
CREATE UNIQUE INDEX UX_employee_shifts_open_slot_per_date_shift
    ON dbo.employee_shifts(shift_date, shift_id)
    WHERE employee_id IS NULL;

----------------------------------------------------------------
-- END OF SCHEMA
----------------------------------------------------------------

-- NOTE: No seed data inserted (per request).
-- If you later want to migrate existing data from the old dbo.schedules into employee_shifts,
-- do ETL/mapping externally taking into account shift/shift_date/start_time/end_time mapping.
