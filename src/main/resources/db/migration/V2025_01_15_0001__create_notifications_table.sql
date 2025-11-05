-- Create notifications table for storing user notifications
CREATE TABLE dbo.notifications (
    notification_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id INT NOT NULL,
    order_id INT NULL,
    title NVARCHAR(255) NOT NULL,
    message NVARCHAR(1000) NOT NULL,
    type NVARCHAR(50) NOT NULL,
    is_read BIT NOT NULL DEFAULT 0,
    created_at DATETIME2(3) NOT NULL DEFAULT GETDATE(),
    
    CONSTRAINT FK_notifications_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id),
    CONSTRAINT FK_notifications_order
        FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id)
);

-- Create index for faster queries
CREATE INDEX IX_notifications_user_created ON dbo.notifications(user_id, created_at DESC);
CREATE INDEX IX_notifications_user_unread ON dbo.notifications(user_id, is_read) WHERE is_read = 0;

