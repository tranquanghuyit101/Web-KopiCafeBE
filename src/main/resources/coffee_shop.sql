------------------------------------------------------------
-- Tạo database
------------------------------------------------------------
IF DB_ID(N'CoffeeShopDB') IS NULL
BEGIN
    CREATE DATABASE [CoffeeShopDB];
END;
GO

USE [CoffeeShopDB];
GO

------------------------------------------------------------
-- Xóa bảng theo thứ tự phụ thuộc (nếu tồn tại)
------------------------------------------------------------
IF OBJECT_ID(N'dbo.inventory_log', N'U') IS NOT NULL DROP TABLE dbo.inventory_log;
IF OBJECT_ID(N'dbo.schedules', N'U') IS NOT NULL DROP TABLE dbo.schedules;
IF OBJECT_ID(N'dbo.payments', N'U') IS NOT NULL DROP TABLE dbo.payments;
IF OBJECT_ID(N'dbo.order_details', N'U') IS NOT NULL DROP TABLE dbo.order_details;
IF OBJECT_ID(N'dbo.orders', N'U') IS NOT NULL DROP TABLE dbo.orders;
IF OBJECT_ID(N'dbo.user_addresses', N'U') IS NOT NULL DROP TABLE dbo.user_addresses;
IF OBJECT_ID(N'dbo.addresses', N'U') IS NOT NULL DROP TABLE dbo.addresses;
IF OBJECT_ID(N'dbo.products', N'U') IS NOT NULL DROP TABLE dbo.products;
IF OBJECT_ID(N'dbo.categories', N'U') IS NOT NULL DROP TABLE dbo.categories;
IF OBJECT_ID(N'dbo.inventory_items', N'U') IS NOT NULL DROP TABLE dbo.inventory_items;
IF OBJECT_ID(N'dbo.roles', N'U') IS NOT NULL DROP TABLE dbo.roles;
IF OBJECT_ID(N'dbo.users', N'U') IS NOT NULL DROP TABLE dbo.users;
GO

------------------------------------------------------------
-- USERS & ROLES
------------------------------------------------------------
CREATE TABLE dbo.users (
    user_id           INT IDENTITY(1,1) PRIMARY KEY,
    username          NVARCHAR(100)  NOT NULL,
    email             NVARCHAR(255)  NULL,
    phone             NVARCHAR(20)   NOT NULL UNIQUE,
    password_hash     NVARCHAR(255)  NOT NULL,
    full_name         NVARCHAR(150)  NOT NULL,
    role_id           INT         NOT NULL,
    status            NVARCHAR(20)   NOT NULL CONSTRAINT CK_users_status CHECK (status IN (N'active', N'banned', N'inactive')),
    created_at        DATETIME2(3)   NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSUTCDATETIME(),
    updated_at        DATETIME2(3)   NOT NULL CONSTRAINT DF_users_updated_at DEFAULT SYSUTCDATETIME(),
    last_login_at     DATETIME2(3)   NULL
);
GO

CREATE UNIQUE INDEX UX_users_username ON dbo.users(username);
CREATE UNIQUE INDEX UX_users_email_notnull ON dbo.users(email) WHERE email IS NOT NULL;
GO

CREATE TABLE dbo.roles (
    role_id     INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(50)   NOT NULL,
    description NVARCHAR(255)  NULL
);
GO

CREATE UNIQUE INDEX UX_roles_name ON dbo.roles(name);
GO

ALTER TABLE dbo.users
    ADD CONSTRAINT FK_users_role FOREIGN KEY (role_id) REFERENCES dbo.roles(role_id);
GO

CREATE INDEX IX_users_role_id ON dbo.users(role_id);
GO

------------------------------------------------------------
-- ADDRESSES
------------------------------------------------------------
CREATE TABLE dbo.addresses (
    address_id      INT IDENTITY(1,1) PRIMARY KEY,
    address_line    NVARCHAR(255)  NOT NULL,
    ward            NVARCHAR(100)  NULL,
    district        NVARCHAR(100)  NULL,
    city            NVARCHAR(100)  NULL,
    postal_code     NVARCHAR(20)   NULL,
    created_at      DATETIME2(3)   NOT NULL CONSTRAINT DF_addresses_created_at DEFAULT SYSUTCDATETIME()
);
GO


------------------------------------------------------------
-- USER_ADDRESSES (Many-to-Many mapping user <-> address)
------------------------------------------------------------
CREATE TABLE dbo.user_addresses (
    user_address_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT        NOT NULL,
    address_id      INT        NOT NULL,
    recipient_name  NVARCHAR(150) NULL,
    is_default      BIT           NOT NULL CONSTRAINT DF_user_addresses_is_default DEFAULT (0),
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT DF_user_addresses_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UX_user_addresses_user_address UNIQUE (user_id, address_id),
    CONSTRAINT FK_user_addresses_user    FOREIGN KEY (user_id)    REFERENCES dbo.users(user_id)    ON DELETE CASCADE,
    CONSTRAINT FK_user_addresses_address FOREIGN KEY (address_id) REFERENCES dbo.addresses(address_id) ON DELETE CASCADE
);
GO

CREATE INDEX IX_user_addresses_user ON dbo.user_addresses(user_id);
CREATE INDEX IX_user_addresses_address ON dbo.user_addresses(address_id);
GO

-- Mỗi user chỉ có 1 địa chỉ mặc định
CREATE UNIQUE INDEX UX_user_addresses_default_per_user
ON dbo.user_addresses(user_id)
WHERE is_default = 1;
GO

------------------------------------------------------------
-- CATEGORIES & PRODUCTS
------------------------------------------------------------
CREATE TABLE dbo.categories (
    category_id  INT IDENTITY(1,1) PRIMARY KEY,
    name         NVARCHAR(100) NOT NULL,
    is_active    BIT           NOT NULL CONSTRAINT DF_categories_is_active DEFAULT (1),
    display_order INT          NOT NULL CONSTRAINT DF_categories_display_order DEFAULT (0)
);
GO

CREATE UNIQUE INDEX UX_categories_name ON dbo.categories(name);
GO

CREATE TABLE dbo.products (
    product_id   INT IDENTITY(1,1) PRIMARY KEY,
    category_id  INT        NOT NULL,
    name         NVARCHAR(150) NOT NULL,
    img_url      NVARCHAR(255) NULL,
    sku          NVARCHAR(50)  NULL,
    price        DECIMAL(18,2) NOT NULL,
    is_available BIT           NOT NULL CONSTRAINT DF_products_is_available DEFAULT (1),
    stock_qty    INT           NOT NULL CONSTRAINT DF_products_stock_qty DEFAULT (0),
    created_at   DATETIME2(3)  NOT NULL CONSTRAINT DF_products_created_at DEFAULT SYSUTCDATETIME(),
    updated_at   DATETIME2(3)  NOT NULL CONSTRAINT DF_products_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_products_category FOREIGN KEY (category_id) REFERENCES dbo.categories(category_id)
);
GO

CREATE UNIQUE INDEX UX_products_sku_notnull ON dbo.products(sku) WHERE sku IS NOT NULL;
CREATE INDEX IX_products_category ON dbo.products(category_id);
CREATE INDEX IX_products_available ON dbo.products(is_available);
GO

------------------------------------------------------------
-- ORDERS & ORDER DETAILS
------------------------------------------------------------
CREATE TABLE dbo.orders (
    order_id           INT IDENTITY(1,1) PRIMARY KEY,
    order_code         NVARCHAR(30)  NOT NULL,
    customer_id        INT        NULL,
    address_id         INT        NULL,
    created_by_user_id INT        NULL,
    status             NVARCHAR(20)  NOT NULL CONSTRAINT DF_orders_status DEFAULT (N'pending')
                        CONSTRAINT CK_orders_status CHECK (status IN (N'pending', N'completed', N'cancelled', N'refused')),
    close_reason       NVARCHAR(500) NULL,
    subtotal_amount    DECIMAL(18,2) NOT NULL CONSTRAINT DF_orders_subtotal DEFAULT (0),
    discount_amount    DECIMAL(18,2) NOT NULL CONSTRAINT DF_orders_discount DEFAULT (0),
    total_amount       AS (CONVERT(DECIMAL(18,2), subtotal_amount - discount_amount)) PERSISTED,
    note               NVARCHAR(500) NULL,
    created_at         DATETIME2(3)  NOT NULL CONSTRAINT DF_orders_created_at DEFAULT SYSUTCDATETIME(),
    updated_at         DATETIME2(3)  NOT NULL CONSTRAINT DF_orders_updated_at DEFAULT SYSUTCDATETIME(),
    closed_at          DATETIME2(3)  NULL,
    CONSTRAINT UX_orders_order_code UNIQUE (order_code),
    CONSTRAINT FK_orders_customer   FOREIGN KEY (customer_id)        REFERENCES dbo.users(user_id),
    CONSTRAINT FK_orders_address    FOREIGN KEY (address_id)         REFERENCES dbo.addresses(address_id),
    CONSTRAINT FK_orders_created_by FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(user_id)
);
GO

CREATE INDEX IX_orders_status_created_at ON dbo.orders(status, created_at DESC);
CREATE INDEX IX_orders_customer ON dbo.orders(customer_id);
CREATE INDEX IX_orders_created_by ON dbo.orders(created_by_user_id);
GO

CREATE TABLE dbo.order_details (
    order_detail_id       INT IDENTITY(1,1) PRIMARY KEY,
    order_id              INT        NOT NULL,
    product_id            INT        NOT NULL,
    product_name_snapshot NVARCHAR(150) NOT NULL,
    unit_price            DECIMAL(18,2) NOT NULL,
    quantity              INT           NOT NULL CONSTRAINT CK_order_details_qty CHECK (quantity > 0),
    line_total            AS (CONVERT(DECIMAL(18,2), unit_price * quantity)) PERSISTED,
    note                  NVARCHAR(255) NULL,
    CONSTRAINT FK_order_details_order   FOREIGN KEY (order_id)   REFERENCES dbo.orders(order_id)   ON DELETE CASCADE,
    CONSTRAINT FK_order_details_product FOREIGN KEY (product_id) REFERENCES dbo.products(product_id)
);
GO

CREATE INDEX IX_order_details_order ON dbo.order_details(order_id);
CREATE INDEX IX_order_details_product ON dbo.order_details(product_id);
GO

------------------------------------------------------------
-- PAYMENTS
------------------------------------------------------------
CREATE TABLE dbo.payments (
    payment_id   INT IDENTITY(1,1) PRIMARY KEY,
    order_id     INT        NOT NULL,
    amount       DECIMAL(18,2) NOT NULL,
    method       NVARCHAR(20)  NOT NULL CONSTRAINT CK_payments_method CHECK (method IN (N'cash', N'banking')),
    status       NVARCHAR(20)  NOT NULL CONSTRAINT DF_payments_status DEFAULT (N'pending')
                                  CONSTRAINT CK_payments_status CHECK (status IN (N'pending', N'paid')),
    txn_ref      NVARCHAR(100) NULL,
    paid_at      DATETIME2(3)  NULL,
    created_at   DATETIME2(3)  NOT NULL CONSTRAINT DF_payments_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_payments_order FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id) ON DELETE CASCADE
);
GO

CREATE UNIQUE INDEX UX_payments_txn_ref_notnull ON dbo.payments(txn_ref) WHERE txn_ref IS NOT NULL;
CREATE INDEX IX_payments_order ON dbo.payments(order_id);
CREATE INDEX IX_payments_status ON dbo.payments(status);
GO

------------------------------------------------------------
-- SCHEDULES (Ca làm)
------------------------------------------------------------
CREATE TABLE dbo.schedules (
    schedule_id  INT IDENTITY(1,1) PRIMARY KEY,
    employee_id  INT        NULL,
    shift_date   DATE          NOT NULL,
    start_time   TIME(0)       NOT NULL,
    end_time     TIME(0)       NOT NULL,
    status       NVARCHAR(20)  NOT NULL CONSTRAINT DF_schedules_status DEFAULT (N'assigned')
                                  CONSTRAINT CK_schedules_status CHECK (status IN (N'assigned', N'completed', N'missed', N'cancelled')),
    notes        NVARCHAR(255) NULL,
    created_at   DATETIME2(3)  NOT NULL CONSTRAINT DF_schedules_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_schedules_employee FOREIGN KEY (employee_id) REFERENCES dbo.users(user_id) ON DELETE SET NULL
);
GO

CREATE UNIQUE INDEX UX_schedules_employee_slot ON dbo.schedules(employee_id, shift_date, start_time, end_time);
CREATE INDEX IX_schedules_employee_date ON dbo.schedules(employee_id, shift_date);
GO

------------------------------------------------------------
-- INVENTORY & INVENTORY LOG
------------------------------------------------------------
CREATE TABLE dbo.inventory_items (
    inventory_item_id INT IDENTITY(1,1) PRIMARY KEY,
    name              NVARCHAR(150) NOT NULL,
    unit              NVARCHAR(20)  NOT NULL, -- ví dụ: g, kg, ml, l, piece
    quantity_on_hand  DECIMAL(18,3) NOT NULL CONSTRAINT DF_inventory_items_qoh DEFAULT (0),
    reorder_level     DECIMAL(18,3) NOT NULL CONSTRAINT DF_inventory_items_reorder DEFAULT (0),
    is_active         BIT           NOT NULL CONSTRAINT DF_inventory_items_is_active DEFAULT (1),
    notes             NVARCHAR(255) NULL,
    created_at        DATETIME2(3)  NOT NULL CONSTRAINT DF_inventory_items_created_at DEFAULT SYSUTCDATETIME(),
    updated_at        DATETIME2(3)  NOT NULL CONSTRAINT DF_inventory_items_updated_at DEFAULT SYSUTCDATETIME()
);
GO

CREATE UNIQUE INDEX UX_inventory_items_name ON dbo.inventory_items(name);
GO

CREATE TABLE dbo.inventory_log (
    inventory_log_id   INT IDENTITY(1,1) PRIMARY KEY,
    inventory_item_id  INT        NOT NULL,
    change_type        NVARCHAR(10)  NOT NULL CONSTRAINT CK_inventory_log_type CHECK (change_type IN (N'IN', N'OUT', N'ADJUST')),
    quantity_change    DECIMAL(18,3) NOT NULL CONSTRAINT CK_inventory_log_qty CHECK (quantity_change <> 0),
    reason             NVARCHAR(255) NULL,
    order_id           INT        NULL,
    created_by_user_id INT        NULL,
    created_at         DATETIME2(3)  NOT NULL CONSTRAINT DF_inventory_log_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_inventory_log_item FOREIGN KEY (inventory_item_id) REFERENCES dbo.inventory_items(inventory_item_id),
    CONSTRAINT FK_inventory_log_order FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id) ON DELETE SET NULL,
    CONSTRAINT FK_inventory_log_user  FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(user_id) ON DELETE SET NULL
);
GO

CREATE INDEX IX_inventory_log_item_created ON dbo.inventory_log(inventory_item_id, created_at);
GO

------------------------------------------------------------
-- SEED dữ liệu cơ bản
------------------------------------------------------------
INSERT INTO dbo.roles(name, description)
VALUES (N'ADMIN', N'Quản trị hệ thống'),
       (N'STAFF', N'Nhân viên'),
       (N'CUSTOMER', N'Khách hàng');
GO 