-- Migration: add position_shift_rules table and required_count, drop unique shift name constraint
SET XACT_ABORT ON;
BEGIN TRAN;

-- Drop unique index on shift name to allow duplicate shift names
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UQ_shift_name' AND object_id = OBJECT_ID(N'dbo.shift'))
BEGIN
    DROP INDEX UQ_shift_name ON dbo.shift;
END

-- Create position_shift_rules table if not exists
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[position_shift_rules]') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.position_shift_rules (
        rule_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        position_id INT NOT NULL,
        shift_id INT NOT NULL,
        is_allowed BIT NOT NULL DEFAULT 0,
        required_count INT NULL DEFAULT 0
    );

    CREATE INDEX IX_position_shift_rules_position ON dbo.position_shift_rules(position_id);
    CREATE INDEX IX_position_shift_rules_shift ON dbo.position_shift_rules(shift_id);

    ALTER TABLE dbo.position_shift_rules
        ADD CONSTRAINT FK_position_shift_rules_position FOREIGN KEY (position_id) REFERENCES dbo.positions(position_id) ON DELETE CASCADE,
            CONSTRAINT FK_position_shift_rules_shift FOREIGN KEY (shift_id) REFERENCES dbo.shift(shift_id) ON DELETE CASCADE;
END

COMMIT TRAN;
