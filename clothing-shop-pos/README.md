# Sapo POS - Backend API (Spring Boot)

Dự án Backend cung cấp các API RESTful cho hệ thống quản lý cửa hàng thời trang (Sapo POS). Hệ thống được xây dựng trên nền tảng Java Spring Boot, tập trung vào bảo mật, hiệu suất, và kiến trúc Module hóa.

## 1. Nền Tảng Công Nghệ (Tech Stack)

- Framework chính: Spring Boot 3.3.6
- Ngôn ngữ: Java 17
- Cơ sở dữ liệu: MySQL 8.x
- ORM: Spring Data JPA, Hibernate
- Bảo mật & Phân quyền: Spring Security, OAuth2 Resource Server (JWT), HandlerInterceptor
- Tài liệu API: SpringDoc OpenAPI (Swagger UI)
- Lọc dữ liệu & Tìm kiếm động: Spring Filter (Turkraft)
- Tiện ích: Lombok, MapStruct (nếu có)

## 2. Kiến Trúc Dự Án

Dự án áp dụng kiến trúc Layered Architecture (Mô hình N-Tier) kết hợp với Domain-Driven Design (DDD) ở mức thư mục.

### 2.1. Cấu Trúc Thư Mục
Các module nghiệp vụ được chia tách rõ ràng tại `src/main/java/com/sapo/mock/clothing/`:
- /auth: Quản lý đăng nhập, phát hành token (JWT) và refresh token.
- /order: Quản lý đơn hàng (POS, bán hàng, giỏ hàng).
- /product: Quản lý hàng hóa, biến thể (Variant), thuộc tính sản phẩm.
- /customer: CRM, quản lý thông tin khách hàng, hạng thành viên, tích điểm, chiến dịch chăm sóc khách hàng (Campaign).
- /receipt: Quản lý phiếu nhập kho, chuyển kho.
- /statistic: Xử lý truy vấn thống kê, báo cáo doanh thu, biểu đồ Dashboards.
- /supplier: Quản lý nhà cung cấp phục vụ cho nghiệp vụ nhập kho.
- /admin: Quản lý nhân sự, tích hợp với phân quyền.

### 2.2. Luồng Xử Lý Dữ Liệu
- Controller: Tiếp nhận Request HTTP, validating DTO bằng `jakarta.validation`.
- Service: Chứa logic nghiệp vụ lõi (Business Logic).
- Repository: Xử lý tương tác trực tiếp với cơ sở dữ liệu qua Spring Data JPA và Specification.
- DTO (Data Transfer Object): Tách biệt dữ liệu phơi bày ra API với Entity nằm trong DB để đảm bảo bảo mật và tối ưu JSON payload.

## 3. Bảo Mật & Phân Quyền (Security & Authorization)

Hệ thống áp dụng bảo mật hai lớp:
1. Spring Security Filter Chain: Đảm bảo mọi yêu cầu ngoại trừ WhiteList (đăng nhập, tài liệu API) đều phải cung cấp Token JWT hợp lệ.
2. Role-Based Permission Interceptor: Được triển khai qua `PermissionInterceptor`, chặn các Request để kiểm tra quyền truy cập.
   - ROLE_ADMIN: Toàn quyền hệ thống.
   - ROLE_SALE: Chỉ truy cập API liên quan đến Bán hàng, Khách hàng, Sản phẩm.
   - ROLE_CS: Chỉ truy cập API liên quan đến CRM, Khách hàng, Chiến dịch.
   - ROLE_WH: Chỉ truy cập API liên quan đến Kho, Sản phẩm, Phiếu nhập, Nhà cung cấp.

## 4. Hướng Dẫn Cài Đặt Và Chạy Dự Án

### 4.1. Yêu Cầu Môi Trường
- JDK 17
- Maven 3.8+ hoặc chạy trực tiếp qua Wrapper
- MySQL 8.0+

### 4.2. Thiết Lập Database
1. Tạo cơ sở dữ liệu trong MySQL (Ví dụ: `clothing_shop_pos`).
2. Cập nhật thông tin đăng nhập (username, password) vào file `src/main/resources/application.properties` (hoặc `application.yml`).
   spring.datasource.url=jdbc:mysql://localhost:3306/clothing_shop_pos
   spring.datasource.username=root
   spring.datasource.password=yourpassword

### 4.3. Thiết Lập JWT Secret
Hệ thống sử dụng chuỗi Base64 để ký JWT. Hãy đảm bảo bạn đã cấu hình biến `sapo.jwt.base64-secret` trong file properties bằng một chuỗi Base64 dài tối thiểu 32 bytes để đảm bảo an toàn.

### 4.4. Chạy Ứng Dụng
Sử dụng terminal hoặc công cụ IDE (IntelliJ, Eclipse) để chạy file `ClothingShopPosApplication.java`.
Hoặc dùng lệnh Maven:
mvn spring-boot:run

Server mặc định sẽ khởi chạy trên cổng 8080.

## 5. Tài Liệu API (Swagger UI)

Khi server đang chạy, tài liệu mô tả API tích hợp sẵn sẽ có tại:
http://localhost:8080/swagger-ui.html

Toàn bộ request cần được xác thực bằng cách truyền Token JWT vào header:
Authorization: Bearer <your_token>
