-- Migration: Add is_shipping_fee column to orders table
-- and ensure default constraint for discount_codes.is_shipping_fee

SET NOCOUNT ON;
USE [Kopi];
GO

-- ============================================
-- Add is_shipping_fee column to orders table
-- ============================================
IF NOT EXISTS (
    SELECT 1 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'dbo' 
      AND TABLE_NAME = 'orders' 
      AND COLUMN_NAME = 'is_shipping_fee'
)
BEGIN
    PRINT 'Adding is_shipping_fee column to orders table...';
    ALTER TABLE dbo.orders 
    ADD is_shipping_fee BIT NOT NULL DEFAULT 0;
    PRINT 'Column is_shipping_fee added successfully to orders table.';
END
ELSE
BEGIN
    PRINT 'Column is_shipping_fee already exists in orders table.';
END
GO

-- ============================================
-- Ensure default constraint for discount_codes.is_shipping_fee
-- ============================================
IF EXISTS (
    SELECT 1 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'dbo' 
      AND TABLE_NAME = 'discount_codes' 
      AND COLUMN_NAME = 'is_shipping_fee'
)
BEGIN
    -- Check if default constraint already exists
    IF NOT EXISTS (
        SELECT 1 
        FROM sys.default_constraints 
        WHERE parent_object_id = OBJECT_ID('dbo.discount_codes') 
          AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('dbo.discount_codes'), 'is_shipping_fee', 'ColumnId')
          AND name = 'DF_discount_codes_is_shipping_fee'
    )
    BEGIN
        -- Drop existing default if any (with different name)
        DECLARE @constraintName NVARCHAR(200);
        SELECT @constraintName = name 
        FROM sys.default_constraints 
        WHERE parent_object_id = OBJECT_ID('dbo.discount_codes') 
          AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('dbo.discount_codes'), 'is_shipping_fee', 'ColumnId');
        
        IF @constraintName IS NOT NULL AND @constraintName <> 'DF_discount_codes_is_shipping_fee'
        BEGIN
            DECLARE @sqlDrop NVARCHAR(500) = 'ALTER TABLE dbo.discount_codes DROP CONSTRAINT ' + QUOTENAME(@constraintName);
            EXEC sp_executesql @sqlDrop;
            PRINT 'Dropped existing default constraint: ' + @constraintName;
        END
        
        -- Add default constraint
        ALTER TABLE dbo.discount_codes
        ADD CONSTRAINT DF_discount_codes_is_shipping_fee DEFAULT (0) FOR is_shipping_fee;
        PRINT 'Default constraint DF_discount_codes_is_shipping_fee added successfully.';
    END
    ELSE
    BEGIN
        PRINT 'Default constraint DF_discount_codes_is_shipping_fee already exists.';
    END
END
ELSE
BEGIN
    PRINT 'Column is_shipping_fee does not exist in discount_codes table.';
END
GO

PRINT 'Migration completed successfully.';
GO

