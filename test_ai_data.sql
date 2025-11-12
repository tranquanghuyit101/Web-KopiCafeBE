-- ============================================
-- Test Data for AI Chat Functions
-- Standalone SQL Script - Run this directly in SQL Server
-- ============================================
-- This script inserts sample data for testing AI chat functions:
-- 1. Products with various stock levels (for inventory testing)
-- 2. Orders with payments TODAY (for today's revenue testing)
-- 3. Orders with payments from previous days/weeks/months/years (for revenue analysis)
-- ============================================

SET NOCOUNT ON;
USE [Kopi];
GO

-- ============================================
-- CHECK AND ADD MISSING COLUMN IF NEEDED
-- ============================================
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = 'dbo' 
               AND TABLE_NAME = 'orders' 
               AND COLUMN_NAME = 'shipping_amount')
BEGIN
    PRINT 'Adding missing column shipping_amount to orders table...';
    ALTER TABLE dbo.orders ADD shipping_amount DECIMAL(18,2) NOT NULL DEFAULT 0;
    PRINT 'Column shipping_amount added successfully.';
END
ELSE
BEGIN
    PRINT 'Column shipping_amount already exists.';
END
GO

-- ============================================
-- VARIABLES
-- ============================================
DECLARE @today DATETIME2(3) = CAST(GETDATE() AS DATE);
DECLARE @yesterday DATETIME2(3) = DATEADD(DAY, -1, @today);
DECLARE @lastWeek DATETIME2(3) = DATEADD(DAY, -7, @today);
DECLARE @lastMonth DATETIME2(3) = DATEADD(MONTH, -1, @today);
DECLARE @lastYear DATETIME2(3) = DATEADD(YEAR, -1, @today);
DECLARE @todayStart DATETIME2(3) = CAST(@today AS DATETIME2(3));
DECLARE @threeDaysAgo DATETIME2(3) = DATEADD(DAY, -3, @today);
DECLARE @fiveDaysAgo DATETIME2(3) = DATEADD(DAY, -5, @today);

-- ============================================
-- GET OR CREATE NECESSARY DATA
-- ============================================

-- Get or set category IDs
DECLARE @coffeeCategoryId INT = (SELECT TOP 1 category_id FROM dbo.categories WHERE name LIKE N'%Coffee%' OR name LIKE N'%Cà phê%' ORDER BY category_id);
IF @coffeeCategoryId IS NULL
    SET @coffeeCategoryId = (SELECT TOP 1 category_id FROM dbo.categories ORDER BY category_id);

DECLARE @smoothieCategoryId INT = (SELECT TOP 1 category_id FROM dbo.categories WHERE name = N'Smoothies');
IF @smoothieCategoryId IS NULL
    SET @smoothieCategoryId = (SELECT TOP 1 category_id FROM dbo.categories ORDER BY category_id);

-- Get product IDs
DECLARE @product1Id INT = (SELECT product_id FROM dbo.products ORDER BY product_id OFFSET 0 ROWS FETCH NEXT 1 ROW ONLY);
DECLARE @product2Id INT = (SELECT product_id FROM dbo.products ORDER BY product_id OFFSET 1 ROWS FETCH NEXT 1 ROW ONLY);
DECLARE @product3Id INT = (SELECT product_id FROM dbo.products ORDER BY product_id OFFSET 2 ROWS FETCH NEXT 1 ROW ONLY);
DECLARE @product4Id INT = (SELECT product_id FROM dbo.products ORDER BY product_id OFFSET 3 ROWS FETCH NEXT 1 ROW ONLY);
DECLARE @product5Id INT = (SELECT product_id FROM dbo.products ORDER BY product_id OFFSET 4 ROWS FETCH NEXT 1 ROW ONLY);

-- Get user IDs
DECLARE @customerId INT = (SELECT TOP 1 user_id FROM dbo.users WHERE role_id = (SELECT role_id FROM dbo.roles WHERE name = 'CUSTOMER') OR role_id = (SELECT role_id FROM dbo.roles WHERE name = 'ROLE_CUSTOMER'));
IF @customerId IS NULL
    SET @customerId = (SELECT TOP 1 user_id FROM dbo.users ORDER BY user_id);

DECLARE @adminId INT = (SELECT TOP 1 user_id FROM dbo.users WHERE role_id = (SELECT role_id FROM dbo.roles WHERE name = 'ADMIN') OR role_id = (SELECT role_id FROM dbo.roles WHERE name = 'ROLE_ADMIN'));
IF @adminId IS NULL
    SET @adminId = (SELECT TOP 1 user_id FROM dbo.users ORDER BY user_id);

-- ============================================
-- 1. UPDATE PRODUCTS WITH VARIOUS STOCK LEVELS
-- ============================================
PRINT '============================================';
PRINT '1. Updating products with various stock levels...';
PRINT '============================================';

-- Clean up existing test data first
PRINT '  Cleaning up existing test data...';
DELETE FROM dbo.payments WHERE txn_ref IN (N'TXN-TODAY-1', N'TXN-TODAY-2', N'TXN-TODAY-3', N'TXN-YESTERDAY-1', N'TXN-LASTWEEK-1', N'TXN-LASTMONTH-1', N'TXN-LASTYEAR-1', N'TXN-MONTH-1', N'TXN-MONTH-2');
DELETE FROM dbo.order_details WHERE order_id IN (SELECT order_id FROM dbo.orders WHERE order_code LIKE N'ORD-TEST-%');
DELETE FROM dbo.orders WHERE order_code LIKE N'ORD-TEST-%';
PRINT '  Cleaned up existing test data.';

IF @product1Id IS NOT NULL
BEGIN
    UPDATE dbo.products SET stock_qty = 5 WHERE product_id = @product1Id;
    PRINT '  Product 1 stock set to 5 (low stock)';
END

IF @product2Id IS NOT NULL
BEGIN
    UPDATE dbo.products SET stock_qty = 8 WHERE product_id = @product2Id;
    PRINT '  Product 2 stock set to 8 (low stock)';
END

IF @product3Id IS NOT NULL
BEGIN
    UPDATE dbo.products SET stock_qty = 25 WHERE product_id = @product3Id;
    PRINT '  Product 3 stock set to 25 (medium stock)';
END

IF @product4Id IS NOT NULL
BEGIN
    UPDATE dbo.products SET stock_qty = 50 WHERE product_id = @product4Id;
    PRINT '  Product 4 stock set to 50 (medium stock)';
END

IF @product5Id IS NOT NULL
BEGIN
    UPDATE dbo.products SET stock_qty = 100 WHERE product_id = @product5Id;
    PRINT '  Product 5 stock set to 100 (high stock)';
END

-- ============================================
-- 1.5. INSERT INVENTORY ITEMS (Nguyên liệu kho)
-- ============================================
PRINT '';
PRINT '============================================';
PRINT '1.5. Inserting inventory items (nguyên liệu kho)...';
PRINT '============================================';

-- Clean up existing test inventory items
DELETE FROM dbo.inventory_items WHERE name LIKE N'TEST-%' OR name LIKE N'Test-%';
PRINT '  Cleaned up existing test inventory items.';

-- Insert inventory items (nguyên liệu)
DECLARE @now DATETIME2(3) = SYSUTCDATETIME();

-- Coffee beans
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Cà phê Arabica')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Cà phê Arabica', N'kg', 15.500, 20.000, 1, N'Nguyên liệu chính cho cà phê', @now, @now);
    PRINT '  Inserted: Cà phê Arabica - 15.5 kg (low stock)';
END

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Cà phê Robusta')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Cà phê Robusta', N'kg', 8.200, 15.000, 1, N'Nguyên liệu phụ', @now, @now);
    PRINT '  Inserted: Cà phê Robusta - 8.2 kg (low stock)';
END

-- Milk
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Sữa tươi')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Sữa tươi', N'lít', 25.000, 30.000, 1, N'Sữa tươi nguyên chất', @now, @now);
    PRINT '  Inserted: Sữa tươi - 25 lít';
END

-- Sugar
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Đường trắng')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Đường trắng', N'kg', 50.000, 20.000, 1, N'Đường trắng tinh luyện', @now, @now);
    PRINT '  Inserted: Đường trắng - 50 kg';
END

-- Syrup
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Si-rô vani')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Si-rô vani', N'chai', 5.000, 10.000, 1, N'Si-rô vani cho cà phê', @now, @now);
    PRINT '  Inserted: Si-rô vani - 5 chai (low stock)';
END

-- Cups
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Cốc giấy')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Cốc giấy', N'cái', 200.000, 100.000, 1, N'Cốc giấy dùng một lần', @now, @now);
    PRINT '  Inserted: Cốc giấy - 200 cái';
END

-- Straws
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Ống hút')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Ống hút', N'cái', 500.000, 200.000, 1, N'Ống hút nhựa', @now, @now);
    PRINT '  Inserted: Ống hút - 500 cái';
END

-- Ice
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Đá viên')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Đá viên', N'kg', 100.000, 50.000, 1, N'Đá viên sạch', @now, @now);
    PRINT '  Inserted: Đá viên - 100 kg';
END

PRINT '  Total inventory items inserted: 8';

-- ============================================
-- 2. CREATE ORDERS WITH PAYMENTS FOR TODAY
-- ============================================
PRINT '';
PRINT '============================================';
PRINT '2. Creating orders with payments for TODAY...';
PRINT '============================================';

-- Order 1: Today morning (9 AM)
INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-TODAY-1', @customerId, @adminId, N'COMPLETED', 150000.00, 0.00, 20000.00, N'Test order for today - morning', 
     DATEADD(HOUR, 9, @todayStart), DATEADD(HOUR, 9, @todayStart));
DECLARE @orderToday1Id INT = SCOPE_IDENTITY();

IF @product1Id IS NOT NULL AND @orderToday1Id IS NOT NULL
BEGIN
    DECLARE @product1Name NVARCHAR(150) = (SELECT TOP 1 name FROM dbo.products WHERE product_id = @product1Id);
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderToday1Id, @product1Id, @product1Name, 50000.00, 2, NULL);
END

IF @product2Id IS NOT NULL AND @orderToday1Id IS NOT NULL
BEGIN
    DECLARE @product2Name NVARCHAR(150) = (SELECT TOP 1 name FROM dbo.products WHERE product_id = @product2Id);
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderToday1Id, @product2Id, @product2Name, 50000.00, 1, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderToday1Id, 170000.00, N'CASH', N'paid', N'TXN-TODAY-1', DATEADD(HOUR, 9, @todayStart), DATEADD(HOUR, 9, @todayStart));
PRINT '  Order 1 created: 170,000 VND (9 AM)';

-- Order 2: Today afternoon (2 PM)
INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-TODAY-2', @customerId, @adminId, N'COMPLETED', 120000.00, 10000.00, 15000.00, N'Test order for today - afternoon', 
     DATEADD(HOUR, 14, @todayStart), DATEADD(HOUR, 14, @todayStart));
DECLARE @orderToday2Id INT = SCOPE_IDENTITY();

IF @product3Id IS NOT NULL AND @orderToday2Id IS NOT NULL
BEGIN
    DECLARE @product3Name NVARCHAR(150) = (SELECT TOP 1 name FROM dbo.products WHERE product_id = @product3Id);
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderToday2Id, @product3Id, @product3Name, 40000.00, 3, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderToday2Id, 125000.00, N'BANKING', N'paid', N'TXN-TODAY-2', DATEADD(HOUR, 14, @todayStart), DATEADD(HOUR, 14, @todayStart));
PRINT '  Order 2 created: 125,000 VND (2 PM)';

-- Order 3: Today evening (7 PM)
INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-TODAY-3', @customerId, @adminId, N'COMPLETED', 80000.00, 0.00, 10000.00, N'Test order for today - evening', 
     DATEADD(HOUR, 19, @todayStart), DATEADD(HOUR, 19, @todayStart));
DECLARE @orderToday3Id INT = SCOPE_IDENTITY();

IF @product4Id IS NOT NULL AND @orderToday3Id IS NOT NULL
BEGIN
    DECLARE @product4Name NVARCHAR(150) = (SELECT TOP 1 name FROM dbo.products WHERE product_id = @product4Id);
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderToday3Id, @product4Id, @product4Name, 40000.00, 2, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderToday3Id, 90000.00, N'CASH', N'paid', N'TXN-TODAY-3', DATEADD(HOUR, 19, @todayStart), DATEADD(HOUR, 19, @todayStart));
PRINT '  Order 3 created: 90,000 VND (7 PM)';

PRINT '  Total revenue for TODAY: 385,000 VND';

-- ============================================
-- 3. CREATE ORDERS FOR YESTERDAY
-- ============================================
PRINT '';
PRINT '============================================';
PRINT '3. Creating orders for YESTERDAY...';
PRINT '============================================';

INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-YESTERDAY-1', @customerId, @adminId, N'COMPLETED', 200000.00, 0.00, 25000.00, N'Test order for yesterday', 
     DATEADD(HOUR, 12, @yesterday), DATEADD(HOUR, 12, @yesterday));
DECLARE @orderYesterdayId INT = SCOPE_IDENTITY();

IF @product5Id IS NOT NULL AND @orderYesterdayId IS NOT NULL
BEGIN
    DECLARE @product5Name NVARCHAR(150) = (SELECT TOP 1 name FROM dbo.products WHERE product_id = @product5Id);
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderYesterdayId, @product5Id, @product5Name, 50000.00, 4, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderYesterdayId, 225000.00, N'BANKING', N'paid', N'TXN-YESTERDAY-1', DATEADD(HOUR, 12, @yesterday), DATEADD(HOUR, 12, @yesterday));
PRINT '  Order created: 225,000 VND';

-- ============================================
-- 4. CREATE ORDERS FOR LAST WEEK
-- ============================================
PRINT '';
PRINT '============================================';
PRINT '4. Creating orders for LAST WEEK (7 days ago)...';
PRINT '============================================';

INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-LASTWEEK-1', @customerId, @adminId, N'COMPLETED', 180000.00, 15000.00, 20000.00, N'Test order for last week', 
     DATEADD(HOUR, 10, @lastWeek), DATEADD(HOUR, 10, @lastWeek));
DECLARE @orderLastWeekId INT = SCOPE_IDENTITY();

IF @product1Id IS NOT NULL AND @orderLastWeekId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderLastWeekId, @product1Id, @product1Name, 50000.00, 3, NULL);
END

IF @product2Id IS NOT NULL AND @orderLastWeekId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderLastWeekId, @product2Id, @product2Name, 30000.00, 1, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderLastWeekId, 185000.00, N'CASH', N'paid', N'TXN-LASTWEEK-1', DATEADD(HOUR, 10, @lastWeek), DATEADD(HOUR, 10, @lastWeek));
PRINT '  Order created: 185,000 VND';

-- ============================================
-- 5. CREATE ORDERS FOR LAST MONTH
-- ============================================
PRINT '';
PRINT '============================================';
PRINT '5. Creating orders for LAST MONTH...';
PRINT '============================================';

INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-LASTMONTH-1', @customerId, @adminId, N'COMPLETED', 250000.00, 20000.00, 30000.00, N'Test order for last month', 
     DATEADD(HOUR, 15, @lastMonth), DATEADD(HOUR, 15, @lastMonth));
DECLARE @orderLastMonthId INT = SCOPE_IDENTITY();

IF @product3Id IS NOT NULL AND @orderLastMonthId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderLastMonthId, @product3Id, @product3Name, 40000.00, 5, NULL);
END

IF @product4Id IS NOT NULL AND @orderLastMonthId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderLastMonthId, @product4Id, @product4Name, 50000.00, 1, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderLastMonthId, 260000.00, N'BANKING', N'paid', N'TXN-LASTMONTH-1', DATEADD(HOUR, 15, @lastMonth), DATEADD(HOUR, 15, @lastMonth));
PRINT '  Order created: 260,000 VND';

-- ============================================
-- 6. CREATE ORDERS FOR LAST YEAR
-- ============================================
PRINT '';
PRINT '============================================';
PRINT '6. Creating orders for LAST YEAR...';
PRINT '============================================';

INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-LASTYEAR-1', @customerId, @adminId, N'COMPLETED', 300000.00, 0.00, 35000.00, N'Test order for last year', 
     DATEADD(HOUR, 11, @lastYear), DATEADD(HOUR, 11, @lastYear));
DECLARE @orderLastYearId INT = SCOPE_IDENTITY();

IF @product5Id IS NOT NULL AND @orderLastYearId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderLastYearId, @product5Id, @product5Name, 50000.00, 6, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderLastYearId, 335000.00, N'CASH', N'paid', N'TXN-LASTYEAR-1', DATEADD(HOUR, 11, @lastYear), DATEADD(HOUR, 11, @lastYear));
PRINT '  Order created: 335,000 VND';

-- ============================================
-- 7. CREATE ADDITIONAL ORDERS FOR THIS MONTH
-- ============================================
PRINT '';
PRINT '============================================';
PRINT '7. Creating additional orders for THIS MONTH...';
PRINT '============================================';

-- Order 1: 3 days ago
INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-MONTH-1', @customerId, @adminId, N'COMPLETED', 100000.00, 5000.00, 15000.00, N'Test order - 3 days ago', 
     DATEADD(HOUR, 13, @threeDaysAgo), DATEADD(HOUR, 13, @threeDaysAgo));
DECLARE @orderMonth1Id INT = SCOPE_IDENTITY();

IF @product1Id IS NOT NULL AND @orderMonth1Id IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderMonth1Id, @product1Id, @product1Name, 50000.00, 2, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderMonth1Id, 110000.00, N'CASH', N'paid', N'TXN-MONTH-1', DATEADD(HOUR, 13, @threeDaysAgo), DATEADD(HOUR, 13, @threeDaysAgo));
PRINT '  Order 1 created (3 days ago): 110,000 VND';

-- Order 2: 5 days ago
INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-MONTH-2', @customerId, @adminId, N'COMPLETED', 140000.00, 0.00, 20000.00, N'Test order - 5 days ago', 
     DATEADD(HOUR, 16, @fiveDaysAgo), DATEADD(HOUR, 16, @fiveDaysAgo));
DECLARE @orderMonth2Id INT = SCOPE_IDENTITY();

IF @product2Id IS NOT NULL AND @orderMonth2Id IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderMonth2Id, @product2Id, @product2Name, 50000.00, 2, NULL);
END

IF @product3Id IS NOT NULL AND @orderMonth2Id IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderMonth2Id, @product3Id, @product3Name, 40000.00, 1, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderMonth2Id, 160000.00, N'BANKING', N'paid', N'TXN-MONTH-2', DATEADD(HOUR, 16, @fiveDaysAgo), DATEADD(HOUR, 16, @fiveDaysAgo));
PRINT '  Order 2 created (5 days ago): 160,000 VND';

-- ============================================
-- SUMMARY
-- ============================================
PRINT '';
PRINT '============================================';
PRINT 'TEST DATA INSERTION COMPLETED!';
PRINT '============================================';
PRINT '';
PRINT 'Summary:';
PRINT '  ✓ Products updated with various stock levels:';
PRINT '    - Product 1: 5 (low stock)';
PRINT '    - Product 2: 8 (low stock)';
PRINT '    - Product 3: 25 (medium stock)';
PRINT '    - Product 4: 50 (medium stock)';
PRINT '    - Product 5: 100 (high stock)';
PRINT '';
PRINT '  ✓ Orders created:';
PRINT '    - TODAY: 3 orders (385,000 VND total)';
PRINT '    - YESTERDAY: 1 order (225,000 VND)';
PRINT '    - LAST WEEK: 1 order (185,000 VND)';
PRINT '    - LAST MONTH: 1 order (260,000 VND)';
PRINT '    - LAST YEAR: 1 order (335,000 VND)';
PRINT '    - THIS MONTH (other days): 2 orders (270,000 VND)';
PRINT '';
PRINT '============================================';
PRINT 'TESTING COMMANDS:';
PRINT '============================================';
PRINT 'You can now test these commands in the chatbox:';
PRINT '';
PRINT '  📊 REVENUE:';
PRINT '    - "Xem doanh thu hôm nay"';
PRINT '      → Should show: 385,000 VND (3 orders)';
PRINT '';
PRINT '    - "Xem doanh thu hôm qua"';
PRINT '      → Should show: 225,000 VND (1 order)';
PRINT '';
PRINT '    - "Xem doanh thu theo ngày"';
PRINT '      → Should show last 7 days data';
PRINT '';
PRINT '    - "Xem doanh thu theo tháng"';
PRINT '      → Should show last 6 months data';
PRINT '';
PRINT '    - "Xem doanh thu theo năm"';
PRINT '      → Should show last 6 years data';
PRINT '';
PRINT '  📦 INVENTORY:';
PRINT '    - "Kiểm tra tồn kho"';
PRINT '      → Should show all products with stock levels';
PRINT '      → Should warn about low stock products (< 10)';
PRINT '';
PRINT '    - "Xem sản phẩm sắp hết hàng"';
PRINT '      → Should show products with stock < 10';
PRINT '';
PRINT '  🛒 PRODUCTS:';
PRINT '    - "Xem sản phẩm"';
PRINT '      → Should show all available products';
PRINT '';
PRINT '    - "Tìm sản phẩm cà phê"';
PRINT '      → Should show coffee products';
PRINT '';
PRINT '============================================';

GO

