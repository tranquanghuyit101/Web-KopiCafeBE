
-- thêm cột email_verified (xác thực) vao bảng users
USE [Kopi];
GO
IF COL_LENGTH('dbo.users','email_verified') IS NULL
BEGIN
    ALTER TABLE dbo.users
      ADD email_verified BIT NOT NULL
      CONSTRAINT DF_users_email_verified DEFAULT (0) WITH VALUES;
  
END

-- chuyển các tk active thành đã xác thực 
GO
UPDATE dbo.users
SET email_verified = 1
WHERE LOWER([status]) = N'active';
GO
-- thêm bảng OTP nhận mã code xác thực
IF OBJECT_ID(N'dbo.user_otps', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.user_otps(
        id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        otp_hash NVARCHAR(255) NOT NULL,
        expires_at DATETIME2(3) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_user_otps_created_at DEFAULT (SYSUTCDATETIME())
    );
    ALTER TABLE dbo.user_otps WITH CHECK
      ADD CONSTRAINT FK_user_otps_user FOREIGN KEY(user_id) REFERENCES dbo.users(user_id) ON DELETE CASCADE;
    CREATE INDEX IX_user_otps_user   ON dbo.user_otps(user_id);
    CREATE INDEX IX_user_otps_expiry ON dbo.user_otps(expires_at);
END
GO

-- các lệnh dưới để bỏ gán unique với null 
-- Bỏ index cũ (nếu đang tồn tại)
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_users_phone' AND object_id = OBJECT_ID('dbo.users'))
    DROP INDEX UX_users_phone ON dbo.users;
GO

UPDATE dbo.users SET phone = NULL WHERE phone = '';

-- 2) Xoá unique index cũ (nếu tồn tại)
IF EXISTS (SELECT 1 FROM sys.indexes 
           WHERE name = 'UX_users_phone' 
             AND object_id = OBJECT_ID('dbo.users'))
    DROP INDEX UX_users_phone ON dbo.users;
GO

-- 3) Tạo unique filtered index CHỈ khi có giá trị
CREATE UNIQUE INDEX UX_users_phone
ON dbo.users(phone)
WHERE phone IS NOT NULL;   
GO

--  Chặn nhập chuỗi rỗng về sau
ALTER TABLE dbo.users ADD CONSTRAINT CK_users_phone_not_blank
CHECK (phone IS NULL OR phone <> '');
GO
