-- 1) Add shipping_amount if not exists (with default 0 and NOT NULL)
IF COL_LENGTH('dbo.orders', 'shipping_amount') IS NULL
BEGIN
    ALTER TABLE dbo.orders ADD shipping_amount DECIMAL(18,2) NULL;
    UPDATE dbo.orders SET shipping_amount = 0 WHERE shipping_amount IS NULL;
    ALTER TABLE dbo.orders ALTER COLUMN shipping_amount DECIMAL(18,2) NOT NULL;
    IF NOT EXISTS (
        SELECT 1
        FROM sys.default_constraints dc
        JOIN sys.columns c ON c.default_object_id = dc.object_id
        WHERE dc.parent_object_id = OBJECT_ID('dbo.orders')
          AND c.name = 'shipping_amount'
    )
    BEGIN
        ALTER TABLE dbo.orders ADD CONSTRAINT DF_orders_shipping DEFAULT (0) FOR shipping_amount;
    END
END

-- 2) Ensure non-negative shipping amount
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_orders_shipping_amount'
      AND parent_object_id = OBJECT_ID('dbo.orders')
)
BEGIN
    ALTER TABLE dbo.orders ADD CONSTRAINT CK_orders_shipping_amount CHECK (shipping_amount >= 0);
END

-- 3) Recreate computed column total_amount to include shipping_amount
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.orders') AND name = 'total_amount'
)
BEGIN
    ALTER TABLE dbo.orders DROP COLUMN total_amount;
END

ALTER TABLE dbo.orders
ADD total_amount AS (CONVERT(DECIMAL(18,2), subtotal_amount - discount_amount + shipping_amount)) PERSISTED;
