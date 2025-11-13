-- Test Data for AI Chat Functions
-- This script inserts sample data for testing:
-- 1. Products with various stock levels (for inventory testing)
-- 2. Orders with payments today (for today's revenue testing)
-- 3. Orders with payments from previous days/weeks/months/years (for revenue analysis)

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
GO

DECLARE @today DATETIME2(3) = CAST(GETDATE() AS DATE);
DECLARE @yesterday DATETIME2(3) = DATEADD(DAY, -1, @today);
DECLARE @lastWeek DATETIME2(3) = DATEADD(DAY, -7, @today);
DECLARE @lastMonth DATETIME2(3) = DATEADD(MONTH, -1, @today);
DECLARE @lastYear DATETIME2(3) = DATEADD(YEAR, -1, @today);
DECLARE @todayStart DATETIME2(3) = CAST(@today AS DATETIME2(3));
DECLARE @todayEnd DATETIME2(3) = DATEADD(SECOND, -1, DATEADD(DAY, 1, @todayStart));

-- Get existing products and categories
DECLARE @coffeeCategoryId INT = (SELECT TOP 1 category_id FROM dbo.categories WHERE name LIKE N'%Coffee%' OR name LIKE N'%Cà phê%' ORDER BY category_id);
DECLARE @smoothieCategoryId INT = (SELECT TOP 1 category_id FROM dbo.categories WHERE name = N'Smoothies');
DECLARE @bakeryCategoryId INT = (SELECT TOP 1 category_id FROM dbo.categories WHERE name = N'Bakery');
DECLARE @teaCategoryId INT = (SELECT TOP 1 category_id FROM dbo.categories WHERE name = N'Tea Special');

-- Get first product IDs (at least 5 products needed)
DECLARE @product1Id INT = (SELECT product_id FROM dbo.products ORDER BY product_id OFFSET 0 ROWS FETCH NEXT 1 ROW ONLY);
DECLARE @product2Id INT = (SELECT product_id FROM dbo.products ORDER BY product_id OFFSET 1 ROWS FETCH NEXT 1 ROW ONLY);
DECLARE @product3Id INT = (SELECT product_id FROM dbo.products ORDER BY product_id OFFSET 2 ROWS FETCH NEXT 1 ROW ONLY);
DECLARE @product4Id INT = (SELECT product_id FROM dbo.products ORDER BY product_id OFFSET 3 ROWS FETCH NEXT 1 ROW ONLY);
DECLARE @product5Id INT = (SELECT product_id FROM dbo.products ORDER BY product_id OFFSET 4 ROWS FETCH NEXT 1 ROW ONLY);

-- Insert inventory items (nguyên liệu kho)
DECLARE @now DATETIME2(3) = SYSUTCDATETIME();

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Cà phê Arabica')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Cà phê Arabica', N'kg', 15.500, 20.000, 1, N'Nguyên liệu chính cho cà phê', @now, @now);
END

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Cà phê Robusta')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Cà phê Robusta', N'kg', 8.200, 15.000, 1, N'Nguyên liệu phụ', @now, @now);
END

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Sữa tươi')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Sữa tươi', N'lít', 25.000, 30.000, 1, N'Sữa tươi nguyên chất', @now, @now);
END

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Đường trắng')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Đường trắng', N'kg', 50.000, 20.000, 1, N'Đường trắng tinh luyện', @now, @now);
END

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Si-rô vani')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Si-rô vani', N'chai', 5.000, 10.000, 1, N'Si-rô vani cho cà phê', @now, @now);
END

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Cốc giấy')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Cốc giấy', N'cái', 200.000, 100.000, 1, N'Cốc giấy dùng một lần', @now, @now);
END

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Ống hút')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Ống hút', N'cái', 500.000, 200.000, 1, N'Ống hút nhựa', @now, @now);
END

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE name = N'Đá viên')
BEGIN
    INSERT INTO dbo.inventory_items (name, unit, quantity_on_hand, reorder_level, is_active, notes, created_at, updated_at)
    VALUES (N'Đá viên', N'kg', 100.000, 50.000, 1, N'Đá viên sạch', @now, @now);
END

-- Get a customer user (if exists)
DECLARE @customerId INT = (SELECT TOP 1 user_id FROM dbo.users WHERE role_id = (SELECT role_id FROM dbo.roles WHERE name = 'CUSTOMER') OR role_id = (SELECT role_id FROM dbo.roles WHERE name = 'ROLE_CUSTOMER'));
IF @customerId IS NULL
    SET @customerId = (SELECT TOP 1 user_id FROM dbo.users ORDER BY user_id);

-- Get an admin user (for created_by)
DECLARE @adminId INT = (SELECT TOP 1 user_id FROM dbo.users WHERE role_id = (SELECT role_id FROM dbo.roles WHERE name = 'ADMIN') OR role_id = (SELECT role_id FROM dbo.roles WHERE name = 'ROLE_ADMIN'));
IF @adminId IS NULL
    SET @adminId = (SELECT TOP 1 user_id FROM dbo.users ORDER BY user_id);

-- ============================================
-- 0. CLEAN UP EXISTING TEST DATA
-- ============================================
PRINT 'Cleaning up existing test data...';
DELETE FROM dbo.payments WHERE txn_ref IN (N'TXN-TODAY-1', N'TXN-TODAY-2', N'TXN-TODAY-3', N'TXN-YESTERDAY-1', N'TXN-LASTWEEK-1', N'TXN-LASTMONTH-1', N'TXN-LASTYEAR-1', N'TXN-MONTH-1', N'TXN-MONTH-2');
DELETE FROM dbo.order_details WHERE order_id IN (SELECT order_id FROM dbo.orders WHERE order_code LIKE N'ORD-TEST-%');
DELETE FROM dbo.orders WHERE order_code LIKE N'ORD-TEST-%';
PRINT 'Cleaned up existing test data.';

-- ============================================
-- 1. UPDATE PRODUCTS WITH VARIOUS STOCK LEVELS (for inventory testing)
-- ============================================
PRINT 'Updating products with various stock levels...';

-- Set some products to low stock (< 10)
IF @product1Id IS NOT NULL
BEGIN
    UPDATE dbo.products SET stock_qty = 5 WHERE product_id = @product1Id;
    PRINT 'Product 1 stock set to 5';
END

IF @product2Id IS NOT NULL
BEGIN
    UPDATE dbo.products SET stock_qty = 8 WHERE product_id = @product2Id;
    PRINT 'Product 2 stock set to 8';
END

-- Set some products to medium stock
IF @product3Id IS NOT NULL
BEGIN
    UPDATE dbo.products SET stock_qty = 25 WHERE product_id = @product3Id;
    PRINT 'Product 3 stock set to 25';
END

IF @product4Id IS NOT NULL
BEGIN
    UPDATE dbo.products SET stock_qty = 50 WHERE product_id = @product4Id;
    PRINT 'Product 4 stock set to 50';
END

-- Set some products to high stock
IF @product5Id IS NOT NULL
BEGIN
    UPDATE dbo.products SET stock_qty = 100 WHERE product_id = @product5Id;
    PRINT 'Product 5 stock set to 100';
END

-- ============================================
-- 2. CREATE ORDERS WITH PAYMENTS FOR TODAY (for today's revenue testing)
-- ============================================
PRINT 'Creating orders with payments for today...';

-- Order 1: Today morning
INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-TODAY-1', @customerId, @adminId, N'COMPLETED', 150000.00, 0.00, 20000.00, N'Test order for today - morning', 
     DATEADD(HOUR, 9, @todayStart), DATEADD(HOUR, 9, @todayStart));
DECLARE @orderToday1Id INT = SCOPE_IDENTITY();

IF @product1Id IS NOT NULL AND @orderToday1Id IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderToday1Id, @product1Id, (SELECT name FROM dbo.products WHERE product_id = @product1Id), 50000.00, 2, NULL);
END

IF @product2Id IS NOT NULL AND @orderToday1Id IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderToday1Id, @product2Id, (SELECT name FROM dbo.products WHERE product_id = @product2Id), 50000.00, 1, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderToday1Id, 170000.00, N'CASH', N'paid', N'TXN-TODAY-1', DATEADD(HOUR, 9, @todayStart), DATEADD(HOUR, 9, @todayStart));

-- Order 2: Today afternoon
INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-TODAY-2', @customerId, @adminId, N'COMPLETED', 120000.00, 10000.00, 15000.00, N'Test order for today - afternoon', 
     DATEADD(HOUR, 14, @todayStart), DATEADD(HOUR, 14, @todayStart));
DECLARE @orderToday2Id INT = SCOPE_IDENTITY();

IF @product3Id IS NOT NULL AND @orderToday2Id IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderToday2Id, @product3Id, (SELECT name FROM dbo.products WHERE product_id = @product3Id), 40000.00, 3, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderToday2Id, 125000.00, N'BANKING', N'paid', N'TXN-TODAY-2', DATEADD(HOUR, 14, @todayStart), DATEADD(HOUR, 14, @todayStart));

-- Order 3: Today evening
INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-TODAY-3', @customerId, @adminId, N'COMPLETED', 80000.00, 0.00, 10000.00, N'Test order for today - evening', 
     DATEADD(HOUR, 19, @todayStart), DATEADD(HOUR, 19, @todayStart));
DECLARE @orderToday3Id INT = SCOPE_IDENTITY();

IF @product4Id IS NOT NULL AND @orderToday3Id IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderToday3Id, @product4Id, (SELECT name FROM dbo.products WHERE product_id = @product4Id), 40000.00, 2, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderToday3Id, 90000.00, N'CASH', N'paid', N'TXN-TODAY-3', DATEADD(HOUR, 19, @todayStart), DATEADD(HOUR, 19, @todayStart));

PRINT 'Created 3 orders for today with total revenue: 385000 VND';

-- ============================================
-- 3. CREATE ORDERS WITH PAYMENTS FOR YESTERDAY
-- ============================================
PRINT 'Creating orders with payments for yesterday...';

INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-YESTERDAY-1', @customerId, @adminId, N'COMPLETED', 200000.00, 0.00, 25000.00, N'Test order for yesterday', 
     DATEADD(HOUR, 12, @yesterday), DATEADD(HOUR, 12, @yesterday));
DECLARE @orderYesterdayId INT = SCOPE_IDENTITY();

IF @product5Id IS NOT NULL AND @orderYesterdayId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderYesterdayId, @product5Id, (SELECT name FROM dbo.products WHERE product_id = @product5Id), 50000.00, 4, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderYesterdayId, 225000.00, N'BANKING', N'paid', N'TXN-YESTERDAY-1', DATEADD(HOUR, 12, @yesterday), DATEADD(HOUR, 12, @yesterday));

-- ============================================
-- 4. CREATE ORDERS WITH PAYMENTS FOR LAST WEEK (7 days ago)
-- ============================================
PRINT 'Creating orders with payments for last week...';

INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-LASTWEEK-1', @customerId, @adminId, N'COMPLETED', 180000.00, 15000.00, 20000.00, N'Test order for last week', 
     DATEADD(HOUR, 10, @lastWeek), DATEADD(HOUR, 10, @lastWeek));
DECLARE @orderLastWeekId INT = SCOPE_IDENTITY();

IF @product1Id IS NOT NULL AND @orderLastWeekId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderLastWeekId, @product1Id, (SELECT name FROM dbo.products WHERE product_id = @product1Id), 50000.00, 3, NULL);
END

IF @product2Id IS NOT NULL AND @orderLastWeekId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderLastWeekId, @product2Id, (SELECT name FROM dbo.products WHERE product_id = @product2Id), 30000.00, 1, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderLastWeekId, 185000.00, N'CASH', N'paid', N'TXN-LASTWEEK-1', DATEADD(HOUR, 10, @lastWeek), DATEADD(HOUR, 10, @lastWeek));

-- ============================================
-- 5. CREATE ORDERS WITH PAYMENTS FOR LAST MONTH
-- ============================================
PRINT 'Creating orders with payments for last month...';

INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-LASTMONTH-1', @customerId, @adminId, N'COMPLETED', 250000.00, 20000.00, 30000.00, N'Test order for last month', 
     DATEADD(HOUR, 15, @lastMonth), DATEADD(HOUR, 15, @lastMonth));
DECLARE @orderLastMonthId INT = SCOPE_IDENTITY();

IF @product3Id IS NOT NULL AND @orderLastMonthId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderLastMonthId, @product3Id, (SELECT name FROM dbo.products WHERE product_id = @product3Id), 40000.00, 5, NULL);
END

IF @product4Id IS NOT NULL AND @orderLastMonthId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderLastMonthId, @product4Id, (SELECT name FROM dbo.products WHERE product_id = @product4Id), 50000.00, 1, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderLastMonthId, 260000.00, N'BANKING', N'paid', N'TXN-LASTMONTH-1', DATEADD(HOUR, 15, @lastMonth), DATEADD(HOUR, 15, @lastMonth));

-- ============================================
-- 6. CREATE ORDERS WITH PAYMENTS FOR LAST YEAR
-- ============================================
PRINT 'Creating orders with payments for last year...';

INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-LASTYEAR-1', @customerId, @adminId, N'COMPLETED', 300000.00, 0.00, 35000.00, N'Test order for last year', 
     DATEADD(HOUR, 11, @lastYear), DATEADD(HOUR, 11, @lastYear));
DECLARE @orderLastYearId INT = SCOPE_IDENTITY();

IF @product5Id IS NOT NULL AND @orderLastYearId IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderLastYearId, @product5Id, (SELECT name FROM dbo.products WHERE product_id = @product5Id), 50000.00, 6, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderLastYearId, 335000.00, N'CASH', N'paid', N'TXN-LASTYEAR-1', DATEADD(HOUR, 11, @lastYear), DATEADD(HOUR, 11, @lastYear));

-- ============================================
-- 7. CREATE ADDITIONAL ORDERS FOR THIS MONTH (for monthly revenue testing)
-- ============================================
PRINT 'Creating additional orders for this month...';

-- Order 1: 3 days ago
DECLARE @threeDaysAgo DATETIME2(3) = DATEADD(DAY, -3, @today);
INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-MONTH-1', @customerId, @adminId, N'COMPLETED', 100000.00, 5000.00, 15000.00, N'Test order - 3 days ago', 
     DATEADD(HOUR, 13, @threeDaysAgo), DATEADD(HOUR, 13, @threeDaysAgo));
DECLARE @orderMonth1Id INT = SCOPE_IDENTITY();

IF @product1Id IS NOT NULL AND @orderMonth1Id IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderMonth1Id, @product1Id, (SELECT name FROM dbo.products WHERE product_id = @product1Id), 50000.00, 2, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderMonth1Id, 110000.00, N'CASH', N'paid', N'TXN-MONTH-1', DATEADD(HOUR, 13, @threeDaysAgo), DATEADD(HOUR, 13, @threeDaysAgo));

-- Order 2: 5 days ago
DECLARE @fiveDaysAgo DATETIME2(3) = DATEADD(DAY, -5, @today);
INSERT INTO dbo.orders (order_code, customer_id, created_by_user_id, status, subtotal_amount, discount_amount, shipping_amount, note, created_at, updated_at)
VALUES 
    (N'ORD-TEST-MONTH-2', @customerId, @adminId, N'COMPLETED', 140000.00, 0.00, 20000.00, N'Test order - 5 days ago', 
     DATEADD(HOUR, 16, @fiveDaysAgo), DATEADD(HOUR, 16, @fiveDaysAgo));
DECLARE @orderMonth2Id INT = SCOPE_IDENTITY();

IF @product2Id IS NOT NULL AND @orderMonth2Id IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderMonth2Id, @product2Id, (SELECT name FROM dbo.products WHERE product_id = @product2Id), 50000.00, 2, NULL);
END

IF @product3Id IS NOT NULL AND @orderMonth2Id IS NOT NULL
BEGIN
    INSERT INTO dbo.order_details (order_id, product_id, product_name_snapshot, unit_price, quantity, note)
    VALUES (@orderMonth2Id, @product3Id, (SELECT name FROM dbo.products WHERE product_id = @product3Id), 40000.00, 1, NULL);
END

INSERT INTO dbo.payments (order_id, amount, method, status, txn_ref, paid_at, created_at)
VALUES (@orderMonth2Id, 160000.00, N'BANKING', N'paid', N'TXN-MONTH-2', DATEADD(HOUR, 16, @fiveDaysAgo), DATEADD(HOUR, 16, @fiveDaysAgo));

-- ============================================
-- SUMMARY
-- ============================================
PRINT '============================================';
PRINT 'Test data insertion completed!';
PRINT '============================================';
PRINT 'Summary:';
PRINT '  - Products updated with various stock levels (5, 8, 25, 50, 100)';
PRINT '  - 3 orders created for TODAY (total: 385,000 VND)';
PRINT '  - 1 order created for YESTERDAY (225,000 VND)';
PRINT '  - 1 order created for LAST WEEK (185,000 VND)';
PRINT '  - 1 order created for LAST MONTH (260,000 VND)';
PRINT '  - 1 order created for LAST YEAR (335,000 VND)';
PRINT '  - 2 orders created for THIS MONTH (270,000 VND)';
PRINT '';
PRINT 'You can now test:';
PRINT '  - "Xem doanh thu hôm nay" -> Should show 385,000 VND';
PRINT '  - "Kiểm tra tồn kho" -> Should show products with stock levels';
PRINT '  - "Xem sản phẩm" -> Should show all products';
PRINT '  - "Xem doanh thu theo tháng" -> Should show monthly data';
PRINT '  - "Xem doanh thu theo năm" -> Should show yearly data';
PRINT '============================================';

GO

