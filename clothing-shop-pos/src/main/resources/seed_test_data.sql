CREATE DATABASE IF NOT EXISTS clothing_shop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =============================================
-- Chạy SAU KHI đã tạo DB bằng script chính
-- Password tất cả: 123456
-- hello
-- =============================================

USE clothing_shop_db;

-- 1. Insert 3 tài khoản test (password: 123456)
INSERT INTO user (username, password_hash, full_name, phone, role, is_active, created_at) VALUES
('sale01', '$2a$10$26IhRLEHb3p5ZGoxsLiyCevhGyJyxhgF/eGszXGQyJxFH.QQJD9ya', 'NV Ban Hang',  '0901111111', 'ROLE_SALE', TRUE, NOW()),
('cs01',   '$2a$10$26IhRLEHb3p5ZGoxsLiyCevhGyJyxhgF/eGszXGQyJxFH.QQJD9ya', 'NV CSKH',      '0902222222', 'ROLE_SALE',   TRUE, NOW()),
('wh01',   '$2a$10$26IhRLEHb3p5ZGoxsLiyCevhGyJyxhgF/eGszXGQyJxFH.QQJD9ya', 'NV Kho',       '0903333333', 'ROLE_WH',   TRUE, NOW());

-- =============================================
-- TEST: POST http://localhost:8080/api/v1/auth/login
-- Body: { "username": "sale01", "password": "123456" }
-- =============================================

-- 2. Tạo Category test
INSERT IGNORE INTO category (name, is_active, is_deleted) VALUES 
('Áo Thun', TRUE, FALSE);

-- 3. Tạo Product test
INSERT IGNORE INTO product (name, description, category_id, is_deleted, created_at, created_by) VALUES 
('Áo Thun Basic Trắng', 'Áo thun cotton 100% thoải mái', 1, FALSE, NOW(), 1);

-- 4. Tạo Product Variant test
INSERT IGNORE INTO product_variant (product_id, sku, sale_price, import_price, low_stock_threshold, quantity) VALUES 
(1, 'AT-TRANG-M', 150000, 80000, 5, 50);

-- 5. Tạo Customer Group test
INSERT IGNORE INTO customer_group (name, status, code, min_spending, max_spending, created_at) VALUES 
('Thành viên Mới', 'ACTIVE', 'BRONZE', 0, 1000000, NOW());

-- 6. Tạo Customer test (Có 100 điểm thưởng)
INSERT IGNORE INTO customer (full_name, phone, gender, status, customer_group_id, total_spent, reward_points, created_at) VALUES 
('Nguyễn Văn Khách', '0988888888', 'MALE', 'ACTIVE', 1, 0, 100, NOW());

-- 7. Tạo Voucher (Giảm 50k cho đơn từ 100k)
INSERT IGNORE INTO voucher (name, code, discount_amount, min_order_value, status, created_at) VALUES 
('Voucher tri ân', 'GIAM50K', 50000, 100000, 'ACTIVE', NOW());

-- 8. Tặng Voucher 'GIAM50K' cho Khách hàng Nguyễn Văn Khách
-- Giả sử khách ID=1 và Voucher ID=1
INSERT IGNORE INTO customer_voucher (customer_id, voucher_id, status, received_at, expired_at) VALUES 
(1, 1, 'UNUSED', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY));
