-- ============================================
-- Add Inventory Items (Nguyên liệu kho)
-- Chạy file này để thêm dữ liệu inventory items vào database
-- ============================================

SET NOCOUNT ON;
USE [Kopi];
GO

PRINT '============================================';
PRINT 'Adding Inventory Items (Nguyên liệu kho)...';
PRINT '============================================';

DECLARE @now DATETIME2(3) = SYSUTCDATETIME();

-- Coffee beans
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Cà phê Arabica')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Cà phê Arabica', N'kg', 15.500, 20.000, 1, N'Nguyên liệu chính cho cà phê', @now, @now);
    PRINT '  ✓ Inserted: Cà phê Arabica - 15.5 kg (low stock)';
END
ELSE
BEGIN
    PRINT '  - Cà phê Arabica already exists';
END

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Cà phê Robusta')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Cà phê Robusta', N'kg', 8.200, 15.000, 1, N'Nguyên liệu phụ', @now, @now);
    PRINT '  ✓ Inserted: Cà phê Robusta - 8.2 kg (low stock)';
END
ELSE
BEGIN
    PRINT '  - Cà phê Robusta already exists';
END

-- Milk
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Sữa tươi')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Sữa tươi', N'lít', 25.000, 30.000, 1, N'Sữa tươi nguyên chất', @now, @now);
    PRINT '  ✓ Inserted: Sữa tươi - 25 lít';
END
ELSE
BEGIN
    PRINT '  - Sữa tươi already exists';
END

-- Sugar
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Đường trắng')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Đường trắng', N'kg', 50.000, 20.000, 1, N'Đường trắng tinh luyện', @now, @now);
    PRINT '  ✓ Inserted: Đường trắng - 50 kg';
END
ELSE
BEGIN
    PRINT '  - Đường trắng already exists';
END

-- Syrup
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Si-rô vani')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Si-rô vani', N'chai', 5.000, 10.000, 1, N'Si-rô vani cho cà phê', @now, @now);
    PRINT '  ✓ Inserted: Si-rô vani - 5 chai (low stock)';
END
ELSE
BEGIN
    PRINT '  - Si-rô vani already exists';
END

-- Cups
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Cốc giấy')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Cốc giấy', N'cái', 200.000, 100.000, 1, N'Cốc giấy dùng một lần', @now, @now);
    PRINT '  ✓ Inserted: Cốc giấy - 200 cái';
END
ELSE
BEGIN
    PRINT '  - Cốc giấy already exists';
END

-- Straws
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Ống hút')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Ống hút', N'cái', 500.000, 200.000, 1, N'Ống hút nhựa', @now, @now);
    PRINT '  ✓ Inserted: Ống hút - 500 cái';
END
ELSE
BEGIN
    PRINT '  - Ống hút already exists';
END

-- Ice
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Đá viên')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Đá viên', N'kg', 100.000, 50.000, 1, N'Đá viên sạch', @now, @now);
    PRINT '  ✓ Inserted: Đá viên - 100 kg';
END
ELSE
BEGIN
    PRINT '  - Đá viên already exists';
END

PRINT '';
PRINT '============================================';
PRINT 'Completed!';
PRINT '============================================';
PRINT 'Total inventory items: 8';
PRINT '  - Cà phê Arabica (15.5 kg)';
PRINT '  - Cà phê Robusta (8.2 kg)';
PRINT '  - Sữa tươi (25 lít)';
PRINT '  - Đường trắng (50 kg)';
PRINT '  - Si-rô vani (5 chai)';
PRINT '  - Cốc giấy (200 cái)';
PRINT '  - Ống hút (500 cái)';
PRINT '  - Đá viên (100 kg)';
PRINT '============================================';

GO


