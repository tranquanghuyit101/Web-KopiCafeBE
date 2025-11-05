    -- Add sizes, add-ons, linking tables, and update order_details for size selection
-- SQL Server compatible
------------------------------------------------------------
-- 1) SIZES
------------------------------------------------------------
IF OBJECT_ID(N'dbo.sizes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sizes (
        size_id        INT IDENTITY(1,1) NOT NULL,
        name           NVARCHAR(50)  NOT NULL,
        code           NVARCHAR(20)  NULL,
        display_order  INT           NOT NULL,
        created_at     DATETIME2(3)  NOT NULL,
        updated_at     DATETIME2(3)  NOT NULL,
        CONSTRAINT PK_sizes PRIMARY KEY (size_id)
    );

    ALTER TABLE dbo.sizes
        ADD CONSTRAINT DF_sizes_display_order DEFAULT (0) FOR display_order,
            CONSTRAINT DF_sizes_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
            CONSTRAINT DF_sizes_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;

    CREATE UNIQUE INDEX UX_sizes_name ON dbo.sizes(name);
    CREATE UNIQUE INDEX UX_sizes_code_notnull ON dbo.sizes(code) WHERE code IS NOT NULL;
END

------------------------------------------------------------
-- 2) PRODUCT_SIZES (product <-> size with price per size)
------------------------------------------------------------
IF OBJECT_ID(N'dbo.product_sizes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.product_sizes (
        product_size_id INT IDENTITY(1,1) NOT NULL,
        product_id      INT           NOT NULL,
        size_id         INT           NOT NULL,
        price           DECIMAL(18,2) NOT NULL,
        is_available    BIT           NOT NULL,
        CONSTRAINT PK_product_sizes PRIMARY KEY (product_size_id)
    );

    ALTER TABLE dbo.product_sizes
        ADD CONSTRAINT DF_product_sizes_is_available DEFAULT (1) FOR is_available;

    ALTER TABLE dbo.product_sizes
        ADD CONSTRAINT FK_product_sizes_product FOREIGN KEY (product_id) REFERENCES dbo.products(product_id) ON DELETE CASCADE,
            CONSTRAINT FK_product_sizes_size FOREIGN KEY (size_id) REFERENCES dbo.sizes(size_id) ON DELETE CASCADE;

    CREATE UNIQUE INDEX UX_product_sizes_pair ON dbo.product_sizes(product_id, size_id);
    CREATE INDEX IX_product_sizes_product ON dbo.product_sizes(product_id);
    CREATE INDEX IX_product_sizes_size ON dbo.product_sizes(size_id);
END

------------------------------------------------------------
-- 3) ALTER ORDER_DETAILS: add size info
------------------------------------------------------------
IF COL_LENGTH('dbo.order_details', 'size_id') IS NULL
BEGIN
    ALTER TABLE dbo.order_details ADD size_id INT NULL;
END;

-- size_name_snapshot removed per requirements

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_order_details_size'
)
BEGIN
    ALTER TABLE dbo.order_details
        ADD CONSTRAINT FK_order_details_size FOREIGN KEY (size_id) REFERENCES dbo.sizes(size_id) ON DELETE NO ACTION;
END

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes WHERE name = N'IX_order_details_size' AND object_id = OBJECT_ID(N'dbo.order_details')
)
BEGIN
    CREATE INDEX IX_order_details_size ON dbo.order_details(size_id);
END

------------------------------------------------------------
-- 4) ADD_ONS (topping) and PRODUCT_ADD_ONS (mapping with price)
------------------------------------------------------------
IF OBJECT_ID(N'dbo.add_ons', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.add_ons (
        add_on_id      INT IDENTITY(1,1) NOT NULL,
        name           NVARCHAR(100) NOT NULL,
        display_order  INT          NOT NULL,
        created_at     DATETIME2(3) NOT NULL,
        updated_at     DATETIME2(3) NOT NULL,
        CONSTRAINT PK_add_ons PRIMARY KEY (add_on_id)
    );

    ALTER TABLE dbo.add_ons
        ADD CONSTRAINT DF_add_ons_display_order DEFAULT (0) FOR display_order,
            CONSTRAINT DF_add_ons_created_at DEFAULT SYSUTCDATETIME() FOR created_at,
            CONSTRAINT DF_add_ons_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;

    CREATE UNIQUE INDEX UX_add_ons_name ON dbo.add_ons(name);
END

IF OBJECT_ID(N'dbo.product_add_ons', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.product_add_ons (
        product_add_on_id INT IDENTITY(1,1) NOT NULL,
        product_id        INT           NOT NULL,
        add_on_id         INT           NOT NULL,
        price             DECIMAL(18,2) NOT NULL,
        is_available      BIT           NOT NULL,
        CONSTRAINT PK_product_add_ons PRIMARY KEY (product_add_on_id)
    );

    ALTER TABLE dbo.product_add_ons
        ADD CONSTRAINT DF_product_add_ons_is_available DEFAULT (1) FOR is_available;

    ALTER TABLE dbo.product_add_ons
        ADD CONSTRAINT FK_product_add_ons_product FOREIGN KEY (product_id) REFERENCES dbo.products(product_id) ON DELETE CASCADE,
            CONSTRAINT FK_product_add_ons_addon FOREIGN KEY (add_on_id) REFERENCES dbo.add_ons(add_on_id) ON DELETE CASCADE;

    CREATE UNIQUE INDEX UX_product_add_ons_pair ON dbo.product_add_ons(product_id, add_on_id);
    CREATE INDEX IX_product_add_ons_product ON dbo.product_add_ons(product_id);
    CREATE INDEX IX_product_add_ons_add_on ON dbo.product_add_ons(add_on_id);
END

------------------------------------------------------------
-- 5) ORDER_DETAIL_ADD_ONS (selected add-ons per order line)
------------------------------------------------------------
IF OBJECT_ID(N'dbo.order_detail_add_ons', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.order_detail_add_ons (
        order_detail_add_on_id INT IDENTITY(1,1) NOT NULL,
        order_detail_id        INT           NOT NULL,
        add_on_id              INT           NOT NULL,
        unit_price_snapshot    DECIMAL(18,2) NOT NULL,
        CONSTRAINT PK_order_detail_add_ons PRIMARY KEY (order_detail_add_on_id)
    );

    ALTER TABLE dbo.order_detail_add_ons
        ADD CONSTRAINT FK_order_detail_add_ons_detail FOREIGN KEY (order_detail_id) REFERENCES dbo.order_details(order_detail_id) ON DELETE NO ACTION,
            CONSTRAINT FK_order_detail_add_ons_addon FOREIGN KEY (add_on_id) REFERENCES dbo.add_ons(add_on_id) ON DELETE NO ACTION;

    CREATE INDEX IX_order_detail_add_ons_detail ON dbo.order_detail_add_ons(order_detail_id);
    CREATE INDEX IX_order_detail_add_ons_addon ON dbo.order_detail_add_ons(add_on_id);
END


