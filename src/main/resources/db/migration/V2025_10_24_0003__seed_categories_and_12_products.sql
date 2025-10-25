-- Seed 3 new categories and 12 products (4 each)
-- Categories: Smoothies, Bakery, Tea Special

SET NOCOUNT ON;

-- Insert categories if not exists
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Smoothies')
BEGIN
    INSERT INTO dbo.categories (name, is_active, display_order) VALUES (N'Smoothies', 1, 10);
END
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Bakery')
BEGIN
    INSERT INTO dbo.categories (name, is_active, display_order) VALUES (N'Bakery', 1, 11);
END
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Tea Special')
BEGIN
    INSERT INTO dbo.categories (name, is_active, display_order) VALUES (N'Tea Special', 1, 12);
END

-- Smoothies (4)
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Mango Smoothie')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Mango Smoothie', NULL, N'SM-MANGO', 45000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Smoothies';
END
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Strawberry Smoothie')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Strawberry Smoothie', NULL, N'SM-STRAW', 45000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Smoothies';
END
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Banana Smoothie')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Banana Smoothie', NULL, N'SM-BANANA', 40000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Smoothies';
END
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Blueberry Smoothie')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Blueberry Smoothie', NULL, N'SM-BLUE', 50000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Smoothies';
END

-- Bakery (4)
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Croissant')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Croissant', NULL, N'BK-CROI', 30000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Bakery';
END
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Chocolate Muffin')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Chocolate Muffin', NULL, N'BK-MUF-CHO', 35000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Bakery';
END
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Cheesecake Slice')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Cheesecake Slice', NULL, N'BK-CHEESE', 55000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Bakery';
END
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Garlic Bread')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Garlic Bread', NULL, N'BK-GARLIC', 25000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Bakery';
END

-- Tea Special (4)
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Earl Grey')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Earl Grey', NULL, N'TS-EARL', 30000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Tea Special';
END
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Chamomile')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Chamomile', NULL, N'TS-CHAM', 28000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Tea Special';
END
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Jasmine Green Tea')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Jasmine Green Tea', NULL, N'TS-JASM', 32000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Tea Special';
END
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Oolong Tea')
BEGIN
    INSERT INTO dbo.products (category_id, name, img_url, sku, price, is_available, stock_qty, created_at, updated_at)
    SELECT c.category_id, N'Oolong Tea', NULL, N'TS-OOL', 34000, 1, 100, SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.categories c WHERE c.name = N'Tea Special';
END


