-- Migration: Add dining tables and link to orders
-- Date: 2025-10-24
-- Note: Applies on top of existing schema in kopi_schedule.sql

SET XACT_ABORT ON;
BEGIN TRAN;

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[tables]') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.tables (
        table_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        number INT NOT NULL,
        name NVARCHAR(100) NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT DF_tables_status DEFAULT N'AVAILABLE',
        qr_token NVARCHAR(64) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_tables_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(3) NOT NULL CONSTRAINT DF_tables_updated_at DEFAULT SYSUTCDATETIME()
    );

    CREATE UNIQUE INDEX UX_tables_number ON dbo.tables(number);
    CREATE UNIQUE INDEX UX_tables_qr_token ON dbo.tables(qr_token);
END

-- orders.table_id nullable + FK + index
IF COL_LENGTH('dbo.orders', 'table_id') IS NULL
BEGIN
    ALTER TABLE dbo.orders ADD table_id INT NULL;
END

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_orders_tables_table_id' AND parent_object_id = OBJECT_ID(N'dbo.orders')
)
BEGIN
    ALTER TABLE dbo.orders
        ADD CONSTRAINT FK_orders_tables_table_id FOREIGN KEY (table_id) REFERENCES dbo.tables(table_id);
END

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes WHERE name = N'IX_orders_table_id' AND object_id = OBJECT_ID(N'dbo.orders')
)
BEGIN
    CREATE INDEX IX_orders_table_id ON dbo.orders(table_id);
END

COMMIT TRAN;

