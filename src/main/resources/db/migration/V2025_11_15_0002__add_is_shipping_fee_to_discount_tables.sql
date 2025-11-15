-- Flyway migration: Add is_shipping_fee to discount tables (codes + events)
-- Applies to SQL Server (dbo schema)
-- Safe-guards included to avoid errors if re-run

----------------------------------------------------------------
-- Add column to dbo.discount_codes
----------------------------------------------------------------
IF COL_LENGTH('dbo.discount_codes', 'is_shipping_fee') IS NULL
BEGIN
    ALTER TABLE dbo.discount_codes ADD is_shipping_fee BIT NULL;
    -- Backfill existing rows to 0
    UPDATE dbo.discount_codes SET is_shipping_fee = 0 WHERE is_shipping_fee IS NULL;
    -- Make NOT NULL
    ALTER TABLE dbo.discount_codes ALTER COLUMN is_shipping_fee BIT NOT NULL;
    -- Add DEFAULT constraint
    IF OBJECT_ID('DF_discount_codes_is_shipping_fee', 'D') IS NULL
        ALTER TABLE dbo.discount_codes ADD CONSTRAINT DF_discount_codes_is_shipping_fee DEFAULT (0) FOR is_shipping_fee;
END
GO

----------------------------------------------------------------
-- Add column to dbo.discount_events
----------------------------------------------------------------
IF COL_LENGTH('dbo.discount_events', 'is_shipping_fee') IS NULL
BEGIN
    ALTER TABLE dbo.discount_events ADD is_shipping_fee BIT NULL;
    -- Backfill existing rows to 0
    UPDATE dbo.discount_events SET is_shipping_fee = 0 WHERE is_shipping_fee IS NULL;
    -- Make NOT NULL
    ALTER TABLE dbo.discount_events ALTER COLUMN is_shipping_fee BIT NOT NULL;
    -- Add DEFAULT constraint
    IF OBJECT_ID('DF_discount_events_is_shipping_fee', 'D') IS NULL
        ALTER TABLE dbo.discount_events ADD CONSTRAINT DF_discount_events_is_shipping_fee DEFAULT (0) FOR is_shipping_fee;
END
GO

-- End of migration


