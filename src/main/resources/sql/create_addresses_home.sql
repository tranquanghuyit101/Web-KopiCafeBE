-- Migration: create addresses_home table
CREATE TABLE dbo.addresses_home (
    address_id INT IDENTITY PRIMARY KEY,
    user_id INT NOT NULL,
    street NVARCHAR(255) NULL,
    city NVARCHAR(100) NULL,
    district NVARCHAR(100) NULL,
    created_at DATETIME2(3) NULL,
    CONSTRAINT FK_addresses_home_user FOREIGN KEY (user_id) REFERENCES dbo.users(user_id)
);
