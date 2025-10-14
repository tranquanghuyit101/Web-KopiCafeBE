------------------------------------------------------------
-- 1. POSITIONS
------------------------------------------------------------
CREATE TABLE dbo.positions (
    position_id      INT IDENTITY(1,1) NOT NULL,
    position_name    NVARCHAR(100) NOT NULL,
    description      NVARCHAR(255) NULL,
    is_active        BIT NOT NULL CONSTRAINT DF_positions_is_active DEFAULT (1),
    created_by_user_id INT NULL,
    created_at       DATETIME2(3) NOT NULL CONSTRAINT DF_positions_created_at DEFAULT SYSUTCDATETIME(),
    updated_by_user_id INT NULL,
    updated_at       DATETIME2(3) NULL,
    CONSTRAINT PK_positions PRIMARY KEY (position_id),
    CONSTRAINT UQ_positions_name UNIQUE (position_name)
);
GO

ALTER TABLE dbo.positions
ADD CONSTRAINT FK_positions_created_by FOREIGN KEY (created_by_user_id)
    REFERENCES dbo.users(user_id) ON DELETE SET NULL,
    CONSTRAINT FK_positions_updated_by FOREIGN KEY (updated_by_user_id)
    REFERENCES dbo.users(user_id) ON DELETE SET NULL;
GO

ALTER TABLE dbo.users
ADD position_id INT NULL;
GO

ALTER TABLE dbo.users
ADD CONSTRAINT FK_users_position FOREIGN KEY (position_id)
    REFERENCES dbo.positions(position_id) ON DELETE SET NULL;
GO


------------------------------------------------------------
-- 2. SHIFT (Ca làm)
------------------------------------------------------------
CREATE TABLE dbo.shift (
    shift_id       INT IDENTITY(1,1) NOT NULL,
    shift_name     NVARCHAR(100) NOT NULL,
    start_time     TIME(0) NOT NULL,
    end_time       TIME(0) NOT NULL,
    description    NVARCHAR(255) NULL,
    is_active      BIT NOT NULL CONSTRAINT DF_shift_is_active DEFAULT (1),
    created_by_user_id INT NULL,
    created_at     DATETIME2(3) NOT NULL CONSTRAINT DF_shift_created_at DEFAULT SYSUTCDATETIME(),
    updated_by_user_id INT NULL,
    updated_at     DATETIME2(3) NULL,
    CONSTRAINT PK_shift PRIMARY KEY (shift_id),
    CONSTRAINT UQ_shift_name UNIQUE (shift_name),
    CONSTRAINT CK_shift_time CHECK (start_time < end_time)
);
GO

ALTER TABLE dbo.shift
ADD CONSTRAINT FK_shift_created_by FOREIGN KEY (created_by_user_id)
    REFERENCES dbo.users(user_id) ON DELETE SET NULL,
    CONSTRAINT FK_shift_updated_by FOREIGN KEY (updated_by_user_id)
    REFERENCES dbo.users(user_id) ON DELETE SET NULL;
GO


------------------------------------------------------------
-- 3. POSITION-SHIFT RULES
------------------------------------------------------------
CREATE TABLE dbo.position_shift_rules (
    rule_id       INT IDENTITY(1,1) NOT NULL,
    position_id   INT NOT NULL,
    shift_id      INT NOT NULL,
    is_allowed    BIT NOT NULL CONSTRAINT DF_position_shift_rules_allowed DEFAULT (1),
    CONSTRAINT PK_position_shift_rules PRIMARY KEY (rule_id),
    CONSTRAINT FK_position_shift_rules_position FOREIGN KEY (position_id)
        REFERENCES dbo.positions(position_id) ON DELETE CASCADE,
    CONSTRAINT FK_position_shift_rules_shift FOREIGN KEY (shift_id)
        REFERENCES dbo.shift(shift_id) ON DELETE CASCADE,
    CONSTRAINT UQ_position_shift UNIQUE (position_id, shift_id)
);
GO


------------------------------------------------------------
-- 4. RECURRENCE PATTERNS (Lịch lặp lại)
------------------------------------------------------------
CREATE TABLE dbo.recurrence_patterns (
    recurrence_id   INT IDENTITY(1,1) NOT NULL,
    recurrence_type NVARCHAR(20) NOT NULL CHECK (recurrence_type IN (N'none', N'daily', N'weekly', N'monthly')),
    day_of_week     NVARCHAR(10) NULL,   -- ví dụ 'Mon', 'Tue' nếu weekly
    interval_days   INT NULL,            -- ví dụ: mỗi 2 ngày
    CONSTRAINT PK_recurrence_patterns PRIMARY KEY (recurrence_id)
);
GO


------------------------------------------------------------
-- 5. WORK SCHEDULES
------------------------------------------------------------
CREATE TABLE dbo.work_schedules (
    work_schedule_id  INT IDENTITY(1,1) NOT NULL,
    name              NVARCHAR(150) NOT NULL,
    description       NVARCHAR(255) NULL,
    start_date        DATE NOT NULL,
    end_date          DATE NOT NULL,
    recurrence_id     INT NULL,
    created_by_user_id INT NULL,
    created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_work_schedules_created_at DEFAULT SYSUTCDATETIME(),
    updated_by_user_id INT NULL,
    updated_at        DATETIME2(3) NULL,
    CONSTRAINT PK_work_schedules PRIMARY KEY (work_schedule_id),
    CONSTRAINT CK_work_schedules_date CHECK (start_date <= end_date),
    CONSTRAINT FK_work_schedules_recurrence FOREIGN KEY (recurrence_id)
        REFERENCES dbo.recurrence_patterns(recurrence_id) ON DELETE SET NULL,
    CONSTRAINT FK_work_schedules_created_by FOREIGN KEY (created_by_user_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL,
    CONSTRAINT FK_work_schedules_updated_by FOREIGN KEY (updated_by_user_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL
);
GO


------------------------------------------------------------
-- 6. EMPLOYEE SHIFTS
------------------------------------------------------------
CREATE TABLE dbo.employee_shifts (
    employee_shift_id INT IDENTITY(1,1) NOT NULL,
    work_schedule_id  INT NULL,
    employee_id       INT NULL,
    shift_id          INT NOT NULL,
    shift_date        DATE NOT NULL,
    status            NVARCHAR(20) NOT NULL
        CONSTRAINT CK_employee_shifts_status CHECK (status IN (N'assigned', N'completed', N'missed', N'cancelled', N'swapped', N'on_leave')),
    notes             NVARCHAR(255) NULL,
    override_start_time TIME(0) NULL,
    override_end_time   TIME(0) NULL,
    actual_check_in   DATETIME2(3) NULL,
    actual_check_out  DATETIME2(3) NULL,
    overtime_minutes  INT NULL,
    reason            NVARCHAR(255) NULL,
    created_by_user_id INT NULL,
    created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_employee_shifts_created_at DEFAULT SYSUTCDATETIME(),
    updated_by_user_id INT NULL,
    updated_at        DATETIME2(3) NULL,
    CONSTRAINT PK_employee_shifts PRIMARY KEY (employee_shift_id),
    CONSTRAINT FK_employee_shifts_schedule FOREIGN KEY (work_schedule_id)
        REFERENCES dbo.work_schedules(work_schedule_id) ON DELETE SET NULL,
    CONSTRAINT FK_employee_shifts_employee FOREIGN KEY (employee_id)
        REFERENCES dbo.users(user_id) ON DELETE SET NULL,
    CONSTRAINT FK_employee_shifts_shift FOREIGN KEY (shift_id)
        REFERENCES dbo.shift(shift_id) ON DELETE NO ACTION
);
GO

-- Ngăn trùng lịch theo ca + ngày (nhưng vẫn cho phép ca khác nếu không overlap)
CREATE UNIQUE INDEX UX_employee_shifts_employee_date_shift
    ON dbo.employee_shifts(employee_id, shift_date, shift_id);
GO

-- Thêm index cho tra cứu nhanh
CREATE INDEX IX_employee_shifts_schedule ON dbo.employee_shifts(work_schedule_id);
CREATE INDEX IX_employee_shifts_employee ON dbo.employee_shifts(employee_id);
GO
