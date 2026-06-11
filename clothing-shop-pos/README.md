# Clothing Shop POS — Base Project

> **Khung dự án Spring Boot** cho hệ thống POS cửa hàng quần áo.
> Đã tích hợp sẵn: **JWT Authentication** (Access Token + Refresh Token Cookie), **Phân quyền theo Role (RBAC)**, **Global Exception Handler**, **Swagger UI**.
> Các thành viên clone về và bắt đầu code module của mình trên nền này.

---

## Tech Stack

| Thành phần | Công nghệ | Phiên bản |
|---|---|---|
| Framework | Spring Boot | `3.3.6` |
| Ngôn ngữ | Java | `17` |
| Build Tool | Maven | `3.8+` |
| Database | MySQL | `8.x` |
| Bảo mật | Spring Security + JWT | OAuth2 Resource Server |
| ORM | Spring Data JPA / Hibernate | — |
| API Docs | SpringDoc OpenAPI (Swagger UI) | `2.5.0` |
| Boilerplate | Lombok | — |
| Query Filter | Spring Filter (turkraft) | `3.1.7` |

---

## Yêu Cầu Môi Trường

- **Java 17+**
- **Maven 3.8+** (hoặc dùng `mvnw` / `mvnw.cmd` có sẵn trong project)
- **MySQL 8.x** đang chạy local

---

## Hướng Dẫn Cài Đặt & Chạy

### Bước 1 — Clone dự án

```bash
git clone <repo-url>
cd clothing-shop-pos/clothing-shop-pos
```

### Bước 2 — Tạo database

Chạy lệnh SQL sau trong MySQL client (MySQL Workbench, DBeaver, hoặc terminal):

```sql
CREATE DATABASE clothing_shop_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

> Schema (tạo bảng) phải được chạy thủ công từ script SQL do nhóm cung cấp riêng.  
> File `seed_test_data.sql` dùng để **thêm dữ liệu test** (tài khoản, cột `refresh_token`) **sau khi** đã có schema.

### Bước 3 — Cấu hình secret (bắt buộc)

Tạo hoặc mở file **`src/main/resources/application-secret.properties`** và điền:

```properties
spring.datasource.password=mat_khau_mysql_cua_ban
hoangmelinh.jwt.base64-secret=khoa_jwt_base64_it_nhat_64_ky_tu
```

**Tạo JWT secret nhanh (PowerShell):**

```powershell
[Convert]::ToBase64String(
  [System.Text.Encoding]::UTF8.GetBytes("day-la-khoa-bi-mat-cuc-ky-dai-it-nhat-64-ky-tu-de-dung-cho-jwt-hmac-512")
)
```

> File `application-secret.properties` đã có trong `.gitignore` — **KHÔNG commit** lên Git.

### Bước 4 — Seed dữ liệu test (tùy chọn)

Sau khi DB đã có schema, chạy file:

```
src/main/resources/seed_test_data.sql
```

Script này sẽ:
1. Thêm cột `refresh_token` vào bảng `user`
2. Tạo 3 tài khoản test với password `123456`:

| Username | Vai trò | Họ tên |
|---|---|---|
| `sale01` | `ROLE_SALE` | NV Bán Hàng |
| `cs01` | `ROLE_CS` | NV CSKH |
| `wh01` | `ROLE_WH` | NV Kho |

### Bước 5 — Chạy ứng dụng

```bash
# Dùng Maven Wrapper (không cần cài Maven)
./mvnw spring-boot:run        # Linux / macOS
mvnw.cmd spring-boot:run      # Windows

# Hoặc nếu đã cài Maven
mvn spring-boot:run
```

### Bước 6 — Kiểm tra

| | URL |
|---|---|
| Base API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| Health Check | `http://localhost:8080/actuator/health` |

---

## Cấu Trúc Dự Án

```
src/main/java/com/sapo/mock/clothing/
│
├── ClothingShopPosApplication.java          ← Entry point
│
├── config/                                  ← CẤU HÌNH HỆ THỐNG (không cần sửa)
│   ├── SecurityConfiguration.java           │  JWT filter chain, public routes
│   ├── CorsConfig.java                      │  CORS cho frontend (localhost:3000, 5173)
│   ├── CustomAuthenticationEntryPoint.java  │  Trả lỗi 401 theo format chuẩn
│   ├── UserDetailsCustom.java               │  Load user từ DB cho Spring Security
│   ├── OpenAPIConfig.java                   │  Swagger UI + Bearer token auth
│   ├── DateTimeFormatConfiguration.java     │  Parse ngày giờ ISO-8601 cho Jackson
│   ├── PermissionInterceptor.java           │  Kiểm tra quyền theo role mỗi request
│   └── PermissionInterceptorConfiguration  │  Đăng ký interceptor
│
├── controller/
│   └── AuthController.java                  │  POST /login, /logout, /refresh, GET /account
│
├── service/
│   └── UserService.java                     │  Xác thực user, lưu/xóa refresh token
│
├── domain/
│   ├── entity/
│   │   └── User.java                        │  Entity khớp với bảng `user` trong DB
│   ├── request/
│   │   └── ReqLoginDTO.java                 │  Body đăng nhập { username, password }
│   └── response/
│       ├── ResLoginDTO.java                 │  { access_token, user }
│       └── ResultPaginationDTO.java         │  { meta, result } dùng cho API có phân trang
│
├── repository/
│   └── UserRepository.java
│
└── util/
    ├── SecurityUtil.java                    │  Tạo / giải mã JWT access & refresh token
    ├── FormatRestResponse.java              │  Auto-wrap response → RestResponse
    ├── annotation/
    │   └── ApiMessage.java                  │  Annotation set message cho API response
    ├── constant/
    │   └── RoleEnum.java                    │  ROLE_ADMIN, ROLE_SALE, ROLE_CS, ROLE_WH
    └── error/
        ├── GlobalExceptionHandler.java      │  Bắt tất cả exception → RestResponse
        ├── IdInvalidException.java
        ├── ResourceNotFoundException.java
        ├── StorageException.java
        └── PermissionException.java
```

---

## JWT Authentication — Cách Hoạt Động

### Flow

```
Client                            Server
  │                                 │
  │── POST /api/v1/auth/login ────► │  Body: { username, password }
  │                                 │  → Xác thực với DB
  │◄── access_token + cookie ────── │  → access_token (body) + refresh_token (HttpOnly cookie)
  │                                 │
  │── GET  /api/v1/xxx ───────────► │  Header: Authorization: Bearer <access_token>
  │◄── data ──────────────────────  │  → Trả dữ liệu nếu đúng role
  │                                 │
  │── GET  /api/v1/auth/refresh ──► │  Tự động gửi cookie refresh_token
  │◄── new access_token ─────────── │  → Cấp access_token mới (7 ngày)
  │                                 │
  │── POST /api/v1/auth/logout ───► │  Header: Authorization: Bearer <access_token>
  │◄── 200 OK + clear cookie ─────  │  → Xóa refresh_token trong DB, clear cookie
```

### Token Config (application.properties)

| Token | Thời hạn |
|---|---|
| Access Token | 86400 giây (1 ngày) |
| Refresh Token | 604800 giây (7 ngày) |

### API Auth

| Method | Endpoint | Mô tả | Cần Auth? |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Đăng nhập | Không |
| `GET` | `/api/v1/auth/account` | Lấy thông tin tài khoản hiện tại | Bearer token |
| `GET` | `/api/v1/auth/refresh` | Cấp access token mới qua cookie | Không (dùng cookie) |
| `POST` | `/api/v1/auth/logout` | Đăng xuất, xóa refresh token | Bearer token |

---

## Ví Dụ Gọi API

### Đăng nhập

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "sale01", "password": "123456"}'
```

Response thành công:
```json
{
  "statusCode": 200,
  "error": null,
  "message": "Đăng nhập thành công",
  "data": {
    "access_token": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 1,
      "username": "sale01",
      "fullName": "NV Ban Hang",
      "role": "ROLE_SALE"
    }
  }
}
```

### Gọi API có bảo mật

```bash
curl -X GET http://localhost:8080/api/v1/auth/account \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

---

## Response Format Chuẩn

**Mọi API đều tự động trả về format sau** (nhờ `FormatRestResponse`):

### Thành công
```json
{
  "statusCode": 200,
  "error": null,
  "message": "Lấy danh sách thành công",
  "data": { "..." }
}
```

### Lỗi validation (400)
```json
{
  "statusCode": 400,
  "error": "Dữ liệu đầu vào không hợp lệ",
  "message": ["username: username không được để trống"],
  "data": null
}
```

### Lỗi không xác thực (401)
```json
{
  "statusCode": 401,
  "error": "Unauthorized",
  "message": "Token không hợp lệ hoặc đã hết hạn",
  "data": null
}
```

### Lỗi không có quyền (403)
```json
{
  "statusCode": 403,
  "error": "Forbidden",
  "message": "Bạn không có quyền thực hiện thao tác này",
  "data": null
}
```

---

## Phân Quyền (Role-based)

Hệ thống sử dụng `RoleEnum` để kiểm soát quyền truy cập:

| Role | Mô tả |
|---|---|
| `ROLE_ADMIN` | Quản trị toàn hệ thống |
| `ROLE_SALE` | Nhân viên bán hàng (POS) |
| `ROLE_CS` | Nhân viên chăm sóc khách hàng (CRM) |
| `ROLE_WH` | Nhân viên kho (Warehouse) |

`PermissionInterceptor` kiểm tra role của người dùng trước mỗi request có bảo mật.

---

## Hướng Dẫn Viết Code Module Mới (Cho Team)

### 1. Tạo Entity

Tạo file trong `domain/entity/`, map với bảng DB đã có sẵn:

```java
@Entity
@Table(name = "ten_bang")
@Getter @Setter
public class TenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên không được để trống")
    private String name;

    // ... các field khác map với cột DB
}
```

### 2. Tạo Repository

```java
@Repository
public interface TenEntityRepository
    extends JpaRepository<TenEntity, Long>, JpaSpecificationExecutor<TenEntity> {
    // Thêm custom query nếu cần
}
```

### 3. Tạo Service

```java
@Service
public class TenEntityService {

    private final TenEntityRepository repo;

    public TenEntityService(TenEntityRepository repo) {
        this.repo = repo;
    }

    /**
     * Lấy danh sách có phân trang.
     *
     * @param pageable thông tin phân trang (page, size, sort)
     * @return ResultPaginationDTO chứa data + meta phân trang
     */
    public ResultPaginationDTO getAll(Pageable pageable) {
        Page<TenEntity> page = repo.findAll(pageable);
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber());
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        ResultPaginationDTO result = new ResultPaginationDTO();
        result.setMeta(meta);
        result.setResult(page.getContent());
        return result;
    }
}
```

### 4. Tạo Controller

```java
@RestController
@RequestMapping("/api/v1/ten-entity")
@Tag(name = "Ten Module", description = "Mô tả module")
public class TenEntityController {

    private final TenEntityService service;

    public TenEntityController(TenEntityService service) {
        this.service = service;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách thành công")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResultPaginationDTO> getAll(Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @PostMapping
    @ApiMessage("Tạo mới thành công")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TenEntity> create(@Valid @RequestBody TenEntity entity) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(entity));
    }
}
```

---

## Quy Tắc Coding (Bắt Buộc)

1. **Tên biến, method dùng tiếng Anh**, có nghĩa rõ ràng
2. **Mọi method trong service phải có JavaDoc** (`@param`, `@return`, mục đích)
3. **Validate dữ liệu trên server** — dùng `@Valid`, `@NotBlank`, `@Min`, `@NotNull`...
4. **API path theo convention**: `GET /api/v1/products`, `POST /api/v1/orders`
5. **Không viết method quá dài** — tách helper method nếu cần
6. **Dùng `@Transactional`** khi thao tác nhiều bảng trong một request
7. **Message lỗi rõ ràng** — dùng `@ApiMessage` và throw exception với message cụ thể

---

## Liên Hệ & Đóng Góp

Dự án thuộc chương trình **Sapo Intern Mock Project**.  
Mọi thắc mắc liên hệ team lead hoặc tạo Issue trên GitHub repository.
