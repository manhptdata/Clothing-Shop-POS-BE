CREATE DATABASE clothing_shop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =============================================
-- Chạy SAU KHI đã tạo DB bằng script chính
-- Password tất cả: 123456
-- hello
-- =============================================

USE clothing_shop_db;

-- 1. Insert 3 tài khoản test (password: 123456)
INSERT INTO user (username, password_hash, full_name, phone, role, is_active, created_at) VALUES
('sale01', '$2a$10$26IhRLEHb3p5ZGoxsLiyCevhGyJyxhgF/eGszXGQyJxFH.QQJD9ya', 'NV Ban Hang',  '0901111111', 'ROLE_SALE', TRUE, NOW()),
('cs01',   '$2a$10$26IhRLEHb3p5ZGoxsLiyCevhGyJyxhgF/eGszXGQyJxFH.QQJD9ya', 'NV CSKH',      '0902222222', 'ROLE_CS',   TRUE, NOW()),
('wh01',   '$2a$10$26IhRLEHb3p5ZGoxsLiyCevhGyJyxhgF/eGszXGQyJxFH.QQJD9ya', 'NV Kho',       '0903333333', 'ROLE_WH',   TRUE, NOW());

-- =============================================
-- TEST: POST http://localhost:8080/api/v1/auth/login
-- Body: { "username": "sale01", "password": "123456" }
-- =============================================
