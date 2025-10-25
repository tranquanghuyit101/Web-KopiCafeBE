-- Seed initial dining tables 1..10 with random qr_token-like placeholders
-- Note: If unique constraint violations occur, adjust or clear existing seed data

SET NOCOUNT ON;

DECLARE @i INT = 1;
WHILE @i <= 10
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.tables WHERE number = @i)
    BEGIN
        INSERT INTO dbo.tables ([number], [name], [status], [qr_token])
        VALUES (@i, CONCAT(N'Table ', @i), N'AVAILABLE', CONVERT(NVARCHAR(64), NEWID()));
    END
    SET @i = @i + 1;
END


