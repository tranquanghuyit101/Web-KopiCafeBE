-- Add latitude/longitude to addresses (idempotent)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE Name = N'latitude' AND Object_ID = Object_ID(N'dbo.addresses')
)
BEGIN
    ALTER TABLE dbo.addresses ADD latitude DECIMAL(10,6) NULL;
END

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE Name = N'longitude' AND Object_ID = Object_ID(N'dbo.addresses')
)
BEGIN
    ALTER TABLE dbo.addresses ADD longitude DECIMAL(10,6) NULL;
END


