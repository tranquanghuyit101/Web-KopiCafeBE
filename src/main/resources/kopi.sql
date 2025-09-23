------------------------------------------------------------
-- Create database
------------------------------------------------------------
CREATE DATABASE [Kopi];
GO

USE [Kopi];
GO

------------------------------------------------------------
-- TABLES (columns only; no constraints here)
------------------------------------------------------------

-- ROLES
CREATE TABLE dbo.roles (
    role_id     INT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(50)   NOT NULL,
    description NVARCHAR(255)  NULL
);
GO

-- USERS (single role via role_id)
CREATE TABLE dbo.users (
    user_id           INT IDENTITY(1,1) NOT NULL,
    username          NVARCHAR(100)  NOT NULL,
    email             NVARCHAR(255)  NULL,
    phone             NVARCHAR(20)   NOT NULL,
    password_hash     NVARCHAR(255)  NOT NULL,
    full_name         NVARCHAR(150)  NOT NULL,
    role_id           INT         NOT NULL,
    status            NVARCHAR(20)   NOT NULL,
    created_at        DATETIME2(3)   NOT NULL,
    updated_at        DATETIME2(3)   NOT NULL,
    last_login_at     DATETIME2(3)   NULL
);
GO

-- ADDRESSES (canonical, no user columns)
CREATE TABLE dbo.addresses (
    address_id      INT IDENTITY(1,1) NOT NULL,
    address_line    NVARCHAR(255)  NOT NULL,
    ward            NVARCHAR(100)  NULL,
    district        NVARCHAR(100)  NULL,
    city            NVARCHAR(100)  NULL,
    postal_code     NVARCHAR(20)   NULL,
    created_at      DATETIME2(3)   NOT NULL
);
GO

-- USER_ADDRESSES (many-to-many user <-> address)
CREATE TABLE dbo.user_addresses (
    user_address_id INT IDENTITY(1,1) NOT NULL,
    user_id         INT        NOT NULL,
    address_id      INT        NOT NULL,
    recipient_name  NVARCHAR(150) NULL,
    is_default      BIT           NOT NULL,
    created_at      DATETIME2(3)  NOT NULL
);
GO

-- CATEGORIES
CREATE TABLE dbo.categories (
    category_id   INT IDENTITY(1,1) NOT NULL,
    name          NVARCHAR(100) NOT NULL,
    is_active     BIT           NOT NULL,
    display_order INT           NOT NULL
);
GO

-- PRODUCTS
CREATE TABLE dbo.products (
    product_id   INT IDENTITY(1,1) NOT NULL,
    category_id  INT        NOT NULL,
    name         NVARCHAR(150) NOT NULL,
    img_url      NVARCHAR(255) NULL,
    sku          NVARCHAR(50)  NULL,
    price        DECIMAL(18,2) NOT NULL,
    is_available BIT           NOT NULL,
    stock_qty    INT           NOT NULL,
    created_at   DATETIME2(3)  NOT NULL,
    updated_at   DATETIME2(3)  NOT NULL
);
GO

-- ORDERS
CREATE TABLE dbo.orders (
    order_id           INT IDENTITY(1,1) NOT NULL,
    order_code         NVARCHAR(30)  NOT NULL,
    customer_id        INT        NULL,
    address_id         INT        NULL,
    created_by_user_id INT        NULL,
    status             NVARCHAR(20)  NOT NULL,
    close_reason       NVARCHAR(500) NULL,
    subtotal_amount    DECIMAL(18,2) NOT NULL,
    discount_amount    DECIMAL(18,2) NOT NULL,
    total_amount       AS (CONVERT(DECIMAL(18,2), subtotal_amount - discount_amount)) PERSISTED,
    note               NVARCHAR(500) NULL,
    created_at         DATETIME2(3)  NOT NULL,
    updated_at         DATETIME2(3)  NOT NULL,
    closed_at          DATETIME2(3)  NULL
);
GO

-- ORDER DETAILS
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
GO

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
GO

-- SCHEDULES (Shifts)
CREATE TABLE dbo.schedules (
    schedule_id  INT IDENTITY(1,1) NOT NULL,
    employee_id  INT        NULL,
    shift_date   DATE          NOT NULL,
    start_time   TIME(0)       NOT NULL,
    end_time     TIME(0)       NOT NULL,
    status       NVARCHAR(20)  NOT NULL,
    notes        NVARCHAR(255) NULL,
    created_at   DATETIME2(3)  NOT NULL
);
GO

-- INVENTORY ITEMS
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
GO

-- INVENTORY LOG
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
GO

------------------------------------------------------------
-- CONSTRAINTS (PK, FK, CHECK, DEFAULT)
------------------------------------------------------------

-- PRIMARY KEYS
ALTER TABLE dbo.roles            ADD CONSTRAINT PK_roles PRIMARY KEY (role_id);
ALTER TABLE dbo.users            ADD CONSTRAINT PK_users PRIMARY KEY (user_id);
ALTER TABLE dbo.addresses        ADD CONSTRAINT PK_addresses PRIMARY KEY (address_id);
ALTER TABLE dbo.user_addresses   ADD CONSTRAINT PK_user_addresses PRIMARY KEY (user_address_id);
ALTER TABLE dbo.categories       ADD CONSTRAINT PK_categories PRIMARY KEY (category_id);
ALTER TABLE dbo.products         ADD CONSTRAINT PK_products PRIMARY KEY (product_id);
ALTER TABLE dbo.orders           ADD CONSTRAINT PK_orders PRIMARY KEY (order_id);
ALTER TABLE dbo.order_details    ADD CONSTRAINT PK_order_details PRIMARY KEY (order_detail_id);
ALTER TABLE dbo.payments         ADD CONSTRAINT PK_payments PRIMARY KEY (payment_id);
ALTER TABLE dbo.schedules        ADD CONSTRAINT PK_schedules PRIMARY KEY (schedule_id);
ALTER TABLE dbo.inventory_items  ADD CONSTRAINT PK_inventory_items PRIMARY KEY (inventory_item_id);
ALTER TABLE dbo.inventory_log    ADD CONSTRAINT PK_inventory_log PRIMARY KEY (inventory_log_id);
GO

-- FOREIGN KEYS
ALTER TABLE dbo.users
    ADD CONSTRAINT FK_users_role FOREIGN KEY (role_id) REFERENCES dbo.roles(role_id);
GO

ALTER TABLE dbo.products
    ADD CONSTRAINT FK_products_category FOREIGN KEY (category_id) REFERENCES dbo.categories(category_id);
GO

ALTER TABLE dbo.orders
    ADD CONSTRAINT FK_orders_customer   FOREIGN KEY (customer_id)        REFERENCES dbo.users(user_id),
        CONSTRAINT FK_orders_address    FOREIGN KEY (address_id)         REFERENCES dbo.addresses(address_id),
        CONSTRAINT FK_orders_created_by FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(user_id);
GO

ALTER TABLE dbo.order_details
    ADD CONSTRAINT FK_order_details_order   FOREIGN KEY (order_id)   REFERENCES dbo.orders(order_id)   ON DELETE CASCADE,
        CONSTRAINT FK_order_details_product FOREIGN KEY (product_id) REFERENCES dbo.products(product_id);
GO

ALTER TABLE dbo.payments
    ADD CONSTRAINT FK_payments_order FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id) ON DELETE CASCADE;
GO

ALTER TABLE dbo.schedules
    ADD CONSTRAINT FK_schedules_employee FOREIGN KEY (employee_id) REFERENCES dbo.users(user_id) ON DELETE SET NULL;
GO

ALTER TABLE dbo.inventory_log
    ADD CONSTRAINT FK_inventory_log_item FOREIGN KEY (inventory_item_id)  REFERENCES dbo.inventory_items(inventory_item_id),
        CONSTRAINT FK_inventory_log_order FOREIGN KEY (order_id)           REFERENCES dbo.orders(order_id) ON DELETE SET NULL,
        CONSTRAINT FK_inventory_log_user  FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(user_id) ON DELETE SET NULL;
GO

ALTER TABLE dbo.user_addresses
    ADD CONSTRAINT FK_user_addresses_user    FOREIGN KEY (user_id)    REFERENCES dbo.users(user_id)    ON DELETE CASCADE,
        CONSTRAINT FK_user_addresses_address FOREIGN KEY (address_id) REFERENCES dbo.addresses(address_id) ON DELETE CASCADE;
GO

-- CHECK CONSTRAINTS
ALTER TABLE dbo.users
    ADD CONSTRAINT CK_users_status CHECK (status IN (N'active', N'banned', N'inactive'));
GO

ALTER TABLE dbo.order_details
    ADD CONSTRAINT CK_order_details_qty CHECK (quantity > 0);
GO

ALTER TABLE dbo.payments
    ADD CONSTRAINT CK_payments_method CHECK (method IN (N'cash', N'banking')),
        CONSTRAINT CK_payments_status CHECK (status IN (N'pending', N'paid'));
GO

ALTER TABLE dbo.schedules
    ADD CONSTRAINT DF_schedules_status CHECK (status IN (N'assigned', N'completed', N'missed', N'cancelled'));
GO

ALTER TABLE dbo.inventory_log
    ADD CONSTRAINT CK_inventory_log_type CHECK (change_type IN (N'IN', N'OUT', N'ADJUST')),
        CONSTRAINT CK_inventory_log_qty  CHECK (quantity_change <> 0);
GO

-- DEFAULT CONSTRAINTS
ALTER TABLE dbo.users
    ADD CONSTRAINT DF_users_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_users_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;
GO

ALTER TABLE dbo.addresses
    ADD CONSTRAINT DF_addresses_created_at DEFAULT SYSUTCDATETIME() FOR created_at;
GO

ALTER TABLE dbo.categories
    ADD CONSTRAINT DF_categories_is_active DEFAULT (1) FOR is_active,
        CONSTRAINT DF_categories_display_order DEFAULT (0) FOR display_order;
GO

ALTER TABLE dbo.products
    ADD CONSTRAINT DF_products_is_available DEFAULT (1) FOR is_available,
        CONSTRAINT DF_products_stock_qty DEFAULT (0) FOR stock_qty,
        CONSTRAINT DF_products_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_products_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;
GO

ALTER TABLE dbo.orders
    ADD CONSTRAINT DF_orders_status DEFAULT (N'pending') FOR status,
        CONSTRAINT DF_orders_subtotal DEFAULT (0) FOR subtotal_amount,
        CONSTRAINT DF_orders_discount DEFAULT (0) FOR discount_amount,
        CONSTRAINT DF_orders_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_orders_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;
GO

ALTER TABLE dbo.payments
    ADD CONSTRAINT DF_payments_status DEFAULT (N'pending') FOR status,
        CONSTRAINT DF_payments_created_at DEFAULT SYSUTCDATETIME() FOR created_at;
GO

ALTER TABLE dbo.schedules
    ADD CONSTRAINT DF_schedules_status_default DEFAULT (N'assigned') FOR status,
        CONSTRAINT DF_schedules_created_at DEFAULT SYSUTCDATETIME() FOR created_at;
GO

ALTER TABLE dbo.inventory_items
    ADD CONSTRAINT DF_inventory_items_qoh DEFAULT (0) FOR quantity_on_hand,
        CONSTRAINT DF_inventory_items_reorder DEFAULT (0) FOR reorder_level,
        CONSTRAINT DF_inventory_items_is_active DEFAULT (1) FOR is_active,
        CONSTRAINT DF_inventory_items_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
        CONSTRAINT DF_inventory_items_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;
GO

ALTER TABLE dbo.inventory_log
    ADD CONSTRAINT DF_inventory_log_created_at DEFAULT SYSUTCDATETIME() FOR created_at;
GO

ALTER TABLE dbo.user_addresses
    ADD CONSTRAINT DF_user_addresses_is_default DEFAULT (0) FOR is_default,
        CONSTRAINT DF_user_addresses_created_at DEFAULT SYSUTCDATETIME() FOR created_at;
GO

------------------------------------------------------------
-- INDEXES & UNIQUE CONSTRAINTS
------------------------------------------------------------

-- USERS
CREATE UNIQUE INDEX UX_users_username ON dbo.users(username);
CREATE UNIQUE INDEX UX_users_email_notnull ON dbo.users(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX UX_users_phone ON dbo.users(phone);
CREATE INDEX IX_users_role_id ON dbo.users(role_id);
GO

-- ROLES
CREATE UNIQUE INDEX UX_roles_name ON dbo.roles(name);
GO

-- ADDRESSES <-> USERS
CREATE UNIQUE INDEX UX_user_addresses_user_address ON dbo.user_addresses(user_id, address_id);
CREATE INDEX IX_user_addresses_user ON dbo.user_addresses(user_id);
CREATE INDEX IX_user_addresses_address ON dbo.user_addresses(address_id);
CREATE UNIQUE INDEX UX_user_addresses_default_per_user ON dbo.user_addresses(user_id) WHERE is_default = 1;
GO

-- CATEGORIES
-- (none additional)
GO

-- PRODUCTS
CREATE UNIQUE INDEX UX_products_sku_notnull ON dbo.products(sku) WHERE sku IS NOT NULL;
CREATE INDEX IX_products_category ON dbo.products(category_id);
CREATE INDEX IX_products_available ON dbo.products(is_available);
GO

-- ORDERS
CREATE UNIQUE INDEX UX_orders_order_code ON dbo.orders(order_code);
CREATE INDEX IX_orders_status_created_at ON dbo.orders(status, created_at DESC);
CREATE INDEX IX_orders_customer ON dbo.orders(customer_id);
CREATE INDEX IX_orders_created_by ON dbo.orders(created_by_user_id);
GO

-- ORDER DETAILS
CREATE INDEX IX_order_details_order ON dbo.order_details(order_id);
CREATE INDEX IX_order_details_product ON dbo.order_details(product_id);
GO

-- PAYMENTS
CREATE UNIQUE INDEX UX_payments_txn_ref_notnull ON dbo.payments(txn_ref) WHERE txn_ref IS NOT NULL;
CREATE INDEX IX_payments_order ON dbo.payments(order_id);
CREATE INDEX IX_payments_status ON dbo.payments(status);
GO

-- SCHEDULES
CREATE UNIQUE INDEX UX_schedules_employee_slot ON dbo.schedules(employee_id, shift_date, start_time, end_time);
CREATE INDEX IX_schedules_employee_date ON dbo.schedules(employee_id, shift_date);
GO

-- INVENTORY LOG
CREATE INDEX IX_inventory_log_item_created ON dbo.inventory_log(inventory_item_id, created_at);
GO

------------------------------------------------------------
-- SEED
------------------------------------------------------------
INSERT INTO dbo.roles(name, description)
VALUES (N'ADMIN', N'Quản trị hệ thống'),
       (N'STAFF', N'Nhân viên'),
       (N'CUSTOMER', N'Khách hàng');
GO 