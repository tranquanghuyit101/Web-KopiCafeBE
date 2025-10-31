-- Add nullable shipper_user_id to orders and FK to users
ALTER TABLE dbo.orders
ADD shipper_user_id INT NULL;

ALTER TABLE dbo.orders
ADD CONSTRAINT FK_orders_shipper_user
FOREIGN KEY (shipper_user_id) REFERENCES dbo.users(user_id);


