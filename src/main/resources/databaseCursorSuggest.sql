------------------------------------------------------------
-- databaseCursorSuggest.sql
-- Purpose: Suggested, guarded DDL to extend scheduling model
-- Notes:
--  - Keeps existing dbo.schedules untouched to avoid breaking dependencies
--  - Adds new entities and constraints with IF NOT EXISTS guards
--  - Tightens checks and adds defaults/indexes for performance & data quality
--  - Safe to run multiple times (idempotent guards)
------------------------------------------------------------
GO

------------------------------------------------------------
-- 0) PREREQUISITES: USERS table exists (from base schema)
------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'dbo')
    EXEC('CREATE SCHEMA dbo');
GO

------------------------------------------------------------
-- 1) POSITIONS
------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1 FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
    WHERE t.name = N'positions' AND s.name = N'dbo')
BEGIN
    CREATE TABLE dbo.positions (
        position_id         INT IDENTITY(1,1) NOT NULL,
        position_name       NVARCHAR(100) NOT NULL,
        description         NVARCHAR(255) NULL,
        is_active           BIT NOT NULL CONSTRAINT DF_positions_is_active DEFAULT (1),
        created_by_user_id  INT NULL,
        created_at          DATETIME2(3) NOT NULL CONSTRAINT DF_positions_created_at DEFAULT SYSUTCDATETIME(),
        updated_by_user_id  INT NULL,
        updated_at          DATETIME2(3) NULL,
        CONSTRAINT PK_positions PRIMARY KEY (position_id),
        CONSTRAINT UQ_positions_name UNIQUE (position_name)
    );
END
GO

-- FKs for positions.created_by_user_id / updated_by_user_id
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_positions_created_by')
BEGIN
    ALTER TABLE dbo.positions WITH CHECK
    ADD CONSTRAINT FK_positions_created_by FOREIGN KEY (created_by_user_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_positions_updated_by')
BEGIN
    ALTER TABLE dbo.positions WITH CHECK
    ADD CONSTRAINT FK_positions_updated_by FOREIGN KEY (updated_by_user_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL;
END
GO

-- Helpful FK indexes
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_positions_created_by' AND object_id = OBJECT_ID(N'dbo.positions'))
    CREATE INDEX IX_positions_created_by ON dbo.positions(created_by_user_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_positions_updated_by' AND object_id = OBJECT_ID(N'dbo.positions'))
    CREATE INDEX IX_positions_updated_by ON dbo.positions(updated_by_user_id);
GO

-- Add users.position_id + FK
IF COL_LENGTH('dbo.users', 'position_id') IS NULL
    ALTER TABLE dbo.users ADD position_id INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_users_position')
BEGIN
    ALTER TABLE dbo.users WITH CHECK
    ADD CONSTRAINT FK_users_position FOREIGN KEY (position_id)
        REFERENCES dbo.positions(position_id) ON DELETE SET NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_users_position_id' AND object_id = OBJECT_ID(N'dbo.users'))
    CREATE INDEX IX_users_position_id ON dbo.users(position_id);
GO

------------------------------------------------------------
-- 2) SHIFT (singular table name maintained to match proposal)
------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1 FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
    WHERE t.name = N'shift' AND s.name = N'dbo')
BEGIN
    CREATE TABLE dbo.shift (
        shift_id           INT IDENTITY(1,1) NOT NULL,
        shift_name         NVARCHAR(100) NOT NULL,
        start_time         TIME(0) NOT NULL,
        end_time           TIME(0) NOT NULL,
        description        NVARCHAR(255) NULL,
        is_active          BIT NOT NULL CONSTRAINT DF_shift_is_active DEFAULT (1),
        created_by_user_id INT NULL,
        created_at         DATETIME2(3) NOT NULL CONSTRAINT DF_shift_created_at DEFAULT SYSUTCDATETIME(),
        updated_by_user_id INT NULL,
        updated_at         DATETIME2(3) NULL,
        CONSTRAINT PK_shift PRIMARY KEY (shift_id),
        CONSTRAINT UQ_shift_name UNIQUE (shift_name),
        CONSTRAINT CK_shift_time CHECK (start_time < end_time)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_shift_created_by')
BEGIN
    ALTER TABLE dbo.shift WITH CHECK
    ADD CONSTRAINT FK_shift_created_by FOREIGN KEY (created_by_user_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_shift_updated_by')
BEGIN
    ALTER TABLE dbo.shift WITH CHECK
    ADD CONSTRAINT FK_shift_updated_by FOREIGN KEY (updated_by_user_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_shift_is_active' AND object_id = OBJECT_ID(N'dbo.shift'))
    CREATE INDEX IX_shift_is_active ON dbo.shift(is_active);
GO

------------------------------------------------------------
-- 3) POSITION-SHIFT RULES
------------------------------------------------------------
-- Removed: position_shift_rules (not present in legacy schema)

------------------------------------------------------------
-- 4) RECURRENCE PATTERNS
------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1 FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
    WHERE t.name = N'recurrence_patterns' AND s.name = N'dbo')
BEGIN
    CREATE TABLE dbo.recurrence_patterns (
        recurrence_id   INT IDENTITY(1,1) NOT NULL,
        recurrence_type NVARCHAR(20) NOT NULL,
        day_of_week     NVARCHAR(10) NULL,
        interval_days   INT NULL,
        CONSTRAINT PK_recurrence_patterns PRIMARY KEY (recurrence_id),
        CONSTRAINT CK_recurrence_type CHECK (recurrence_type IN (N'none', N'daily', N'weekly', N'monthly')),
        -- If weekly -> day_of_week required and must be valid; otherwise day_of_week must be NULL
        CONSTRAINT CK_recurrence_day_of_week
            CHECK ((recurrence_type = N'weekly' AND day_of_week IN (N'Mon',N'Tue',N'Wed',N'Thu',N'Fri',N'Sat',N'Sun'))
                OR (recurrence_type <> N'weekly' AND day_of_week IS NULL)),
        -- interval_days must be positive for repeating types; must be NULL for 'none'
        CONSTRAINT CK_recurrence_interval
            CHECK ((recurrence_type = N'none' AND interval_days IS NULL)
                OR (recurrence_type <> N'none' AND interval_days IS NOT NULL AND interval_days > 0))
    );
END
GO

------------------------------------------------------------
-- 5) WORK SCHEDULES
------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1 FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
    WHERE t.name = N'work_schedules' AND s.name = N'dbo')
BEGIN
    CREATE TABLE dbo.work_schedules (
        work_schedule_id   INT IDENTITY(1,1) NOT NULL,
        name               NVARCHAR(150) NOT NULL,
        description        NVARCHAR(255) NULL,
        start_date         DATE NOT NULL,
        end_date           DATE NOT NULL,
        recurrence_id      INT NULL,
        created_by_user_id INT NULL,
        created_at         DATETIME2(3) NOT NULL CONSTRAINT DF_work_schedules_created_at DEFAULT SYSUTCDATETIME(),
        updated_by_user_id INT NULL,
        updated_at         DATETIME2(3) NULL,
        CONSTRAINT PK_work_schedules PRIMARY KEY (work_schedule_id),
        CONSTRAINT CK_work_schedules_date CHECK (start_date <= end_date)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_work_schedules_recurrence')
BEGIN
    ALTER TABLE dbo.work_schedules WITH CHECK
    ADD CONSTRAINT FK_work_schedules_recurrence FOREIGN KEY (recurrence_id)
        REFERENCES dbo.recurrence_patterns(recurrence_id) ON DELETE SET NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_work_schedules_created_by')
BEGIN
    ALTER TABLE dbo.work_schedules WITH CHECK
    ADD CONSTRAINT FK_work_schedules_created_by FOREIGN KEY (created_by_user_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_work_schedules_updated_by')
BEGIN
    ALTER TABLE dbo.work_schedules WITH CHECK
    ADD CONSTRAINT FK_work_schedules_updated_by FOREIGN KEY (updated_by_user_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_work_schedules_date' AND object_id = OBJECT_ID(N'dbo.work_schedules'))
    CREATE INDEX IX_work_schedules_date ON dbo.work_schedules(start_date, end_date);
GO

------------------------------------------------------------
-- 6) EMPLOYEE SHIFTS
------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1 FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
    WHERE t.name = N'employee_shifts' AND s.name = N'dbo')
BEGIN
    CREATE TABLE dbo.employee_shifts (
        employee_shift_id  INT IDENTITY(1,1) NOT NULL,
        work_schedule_id   INT NULL,
        employee_id        INT NULL, -- allow NULL for unassigned/open shifts
        shift_id           INT NOT NULL,
        shift_date         DATE NOT NULL,
        status             NVARCHAR(20) NOT NULL CONSTRAINT DF_employee_shifts_status DEFAULT (N'assigned'),
        notes              NVARCHAR(255) NULL,
        override_start_time TIME(0) NULL,
        override_end_time   TIME(0) NULL,
        actual_check_in    DATETIME2(3) NULL,
        actual_check_out   DATETIME2(3) NULL,
        overtime_minutes   INT NULL,
        reason             NVARCHAR(255) NULL,
        created_by_user_id INT NULL,
        created_at         DATETIME2(3) NOT NULL CONSTRAINT DF_employee_shifts_created_at DEFAULT SYSUTCDATETIME(),
        updated_by_user_id INT NULL,
        updated_at         DATETIME2(3) NULL,
        CONSTRAINT PK_employee_shifts PRIMARY KEY (employee_shift_id),
        CONSTRAINT CK_employee_shifts_status CHECK (status IN (N'assigned', N'completed', N'missed', N'cancelled', N'swapped', N'on_leave')),
        -- Both override times NULL, or both present and start < end
        CONSTRAINT CK_employee_shifts_override_times
            CHECK ((override_start_time IS NULL AND override_end_time IS NULL)
                OR (override_start_time IS NOT NULL AND override_end_time IS NOT NULL AND override_start_time < override_end_time)),
        -- Actual checkout must be after checkin when both provided
        CONSTRAINT CK_employee_shifts_actual_times
            CHECK (actual_check_in IS NULL OR actual_check_out IS NULL OR actual_check_in <= actual_check_out)
    );
END
GO

-- FKs
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_employee_shifts_schedule')
BEGIN
    ALTER TABLE dbo.employee_shifts WITH CHECK
    ADD CONSTRAINT FK_employee_shifts_schedule FOREIGN KEY (work_schedule_id)
        REFERENCES dbo.work_schedules(work_schedule_id) ON DELETE SET NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_employee_shifts_employee')
BEGIN
    ALTER TABLE dbo.employee_shifts WITH CHECK
    ADD CONSTRAINT FK_employee_shifts_employee FOREIGN KEY (employee_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_employee_shifts_shift')
BEGIN
    ALTER TABLE dbo.employee_shifts WITH CHECK
    ADD CONSTRAINT FK_employee_shifts_shift FOREIGN KEY (shift_id)
        REFERENCES dbo.shift(shift_id) ON DELETE NO ACTION;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_employee_shifts_created_by')
BEGIN
    ALTER TABLE dbo.employee_shifts WITH CHECK
    ADD CONSTRAINT FK_employee_shifts_created_by FOREIGN KEY (created_by_user_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_employee_shifts_updated_by')
BEGIN
    ALTER TABLE dbo.employee_shifts WITH CHECK
    ADD CONSTRAINT FK_employee_shifts_updated_by FOREIGN KEY (updated_by_user_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL;
END
GO

-- Indexes for employee_shifts
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_employee_shifts_schedule' AND object_id = OBJECT_ID(N'dbo.employee_shifts'))
    CREATE INDEX IX_employee_shifts_schedule ON dbo.employee_shifts(work_schedule_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_employee_shifts_employee' AND object_id = OBJECT_ID(N'dbo.employee_shifts'))
    CREATE INDEX IX_employee_shifts_employee ON dbo.employee_shifts(employee_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_employee_shifts_shift' AND object_id = OBJECT_ID(N'dbo.employee_shifts'))
    CREATE INDEX IX_employee_shifts_shift ON dbo.employee_shifts(shift_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_employee_shifts_date' AND object_id = OBJECT_ID(N'dbo.employee_shifts'))
    CREATE INDEX IX_employee_shifts_date ON dbo.employee_shifts(shift_date);
GO

-- Unique assignment per employee per (date, shift) for assigned shifts only
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_employee_shifts_employee_date_shift' AND object_id = OBJECT_ID(N'dbo.employee_shifts'))
    CREATE UNIQUE INDEX UX_employee_shifts_employee_date_shift
        ON dbo.employee_shifts(employee_id, shift_date, shift_id)
        WHERE employee_id IS NOT NULL;
GO

-- OPTIONAL: ensure only one open (unassigned) slot per (date, shift). Comment out if multiple open slots are needed.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_employee_shifts_open_slot_per_date_shift' AND object_id = OBJECT_ID(N'dbo.employee_shifts'))
    CREATE UNIQUE INDEX UX_employee_shifts_open_slot_per_date_shift
        ON dbo.employee_shifts(shift_date, shift_id)
        WHERE employee_id IS NULL;
GO

------------------------------------------------------------
-- 7) COMPAT WITH EXISTING dbo.schedules
-- This script intentionally DOES NOT drop/alter dbo.schedules.
-- Migrations from dbo.schedules to employee_shifts (if desired) should be done via ETL.
------------------------------------------------------------
-- Example seed (optional): create basic positions and shifts only if empty
IF NOT EXISTS (SELECT 1 FROM dbo.positions)
BEGIN
    INSERT INTO dbo.positions(position_name, description)
    VALUES (N'Barista', N'Pha chế'), (N'Cashier', N'Thu ngân'), (N'Manager', N'Quản lý');
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.shift)
BEGIN
    INSERT INTO dbo.shift(shift_name, start_time, end_time, description)
    VALUES (N'Sáng', '07:00', '12:00', N'Ca sáng'),
           (N'Chiều', '12:00', '17:00', N'Ca chiều'),
           (N'Tối',  '17:00', '22:00', N'Ca tối');
END
GO

------------------------------------------------------------
-- END
------------------------------------------------------------
GO


