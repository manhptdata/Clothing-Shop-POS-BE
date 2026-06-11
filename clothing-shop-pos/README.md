# Clothing Shop POS — Base Project

> Khung dự án Spring Boot cho hệ thống POS cửa hàng quần áo.
> Đã tích hợp sẵn: **JWT Authentication**, **Phân quyền RBAC**, **Global Exception Handler**, **Swagger UI**.
> Các thành viên clone về và bắt đầu code module của mình.

---

## Tech Stack

| Thành phần | Công nghệ | Phiên bản |
|---|---|---|
| Framework | Spring Boot | `3.3.6` |
| Ngôn ngữ | Java | `17` |
| Build Tool | Maven | — |
| Database | MySQL | — |
| Bảo mật | Spring Security + JWT | OAuth2 Resource Server |
| ORM | Spring Data JPA | Hibernate |
| API Docs | SpringDoc OpenAPI | Swagger UI `2.5.0` |
| Boilerplate | Lombok | — |

---

## Hướng Dẫn Chạy (Bắt Buộc Đọc)

### Bước 1 — Cài đặt

- Java 17+
- Maven 3.8+ (hoặc dùng `./mvnw` có sẵn)
- MySQL đang chạy

### Bước 2 — Tạo database

```sql
CREATE DATABASE clothing_shop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Bước 3 — Cấu hình secret

Mở file `src/main/resources/application-secret.properties` và điền:

```properties
spring.datasource.password=mat_khau_mysql_cua_ban
hoangmelinh.jwt.base64-secret=khoa_bi_mat_base64_it_nhat_64_ky_tu
```

> **Tạo JWT secret nhanh (PowerShell):**
> ```powershell
> [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("day-la-khoa-bi-mat-cuc-ky-dai-it-nhat-64-ky-tu-de-dung-cho-jwt-hmac-512"))
> ```

> File này đã được `.gitignore` — **KHÔNG commit** lên git.

### Bước 4 — Chạy

```bash
./mvnw spring-boot:run
```

### Bước 5 — Kiểm tra

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## Cấu Trúc Dự Án Hiện Tại

```
com.sapo.mock.clothing/
│
├── ClothingShopPosApplication.java          ← Entry point
│
├── config/                                   ← CẤU HÌNH HỆ THỐNG (đã xong, không cần sửa)
│   ├── SecurityConfiguration.java            │  Spring Security + JWT filter chain
│   ├── CorsConfig.java                       │  CORS cho frontend (localhost:3000, 5173)
│   ├── CustomAuthenticationEntryPoint.java    │  Trả lỗi 401 theo format chuẩn
│   ├── UserDetailsCustom.java                │  Load user từ DB cho Spring Security
│   ├── JpaAuditingConfig.java                │  Tự động điền createdBy / updatedBy
│   ├── OpenAPIConfig.java                    │  Swagger UI + Bearer token
│   ├── DateTimeFormatConfiguration.java      │  Format ngày giờ ISO-8601
│   ├── StaticResourcesWebConfiguration.java  │  Serve file upload qua /storage/**
│   ├── PermissionInterceptor.java            │  Kiểm tra quyền từ DB mỗi request
│   └── PermissionInterceptorConfiguration.java  Đăng ký interceptor
│
├── controller/                               ← CONTROLLER
│   └── AuthController.java                   │  POST /login, /logout, /refresh, GET /account
│
├── service/                                  ← SERVICE
│   ├── UserService.java                      │  CRUD user, hash password, refresh token
│   └── RoleService.java                      │  CRUD role
│
├── domain/
│   ├── entity/                               ← ENTITY (JPA)
│   │   ├── BaseEntity.java                   │  Lớp cha: createdAt, updatedAt, createdBy, updatedBy
│   │   ├── User.java                         │  Người dùng hệ thống
│   │   ├── Role.java                         │  Vai trò (Admin, NV Bán hàng, NV CSKH, NV Kho)
│   │   └── Permission.java                   │  Quyền: apiPath + method + module
│   ├── request/
│   │   └── ReqLoginDTO.java                  │  { username, password }
│   └── response/
│       ├── RestResponse.java                 │  { statusCode, error, message, data }
│       ├── ResLoginDTO.java                  │  { access_token, user }
│       └── ResultPaginationDTO.java          │  { meta, result }
│
├── repository/                               ← REPOSITORY
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   └── PermissionRepository.java
│
└── util/                                     ← UTILITIES
    ├── SecurityUtil.java                     │  Tạo/decode JWT, lấy user hiện tại
    ├── FormatRestResponse.java               │  Auto-wrap response → RestResponse
    ├── annotation/
    │   └── ApiMessage.java                   │  Annotation đặt message cho API
    ├── constant/
    │   └── GenderEnum.java                   │  MALE, FEMALE, OTHER
    └── error/
        ├── GlobalExceptionHandler.java       │  Bắt tất cả exception → RestResponse
        ├── IdInvalidException.java
        ├── ResourceNotFoundException.java
        ├── StorageException.java
        └── PermissionException.java
```

---

## JWT Authentication — Cách Hoạt Động

### Flow đăng nhập

```
Client                          Server
  │                               │
  │── POST /api/v1/auth/login ──► │  Gửi { username, password }
  │                               │  → Xác thực với DB
  │◄── access_token + cookie ──── │  → Trả access_token (body) + refresh_token (cookie)
  │                               │
  │── GET /api/v1/xxx ──────────► │  Gửi kèm header: Authorization: Bearer <access_token>
  │◄── data ─────────────────── │  → Trả dữ liệu nếu có quyền
  │                               │
  │── GET /api/v1/auth/refresh ─► │  Gửi cookie refresh_token
  │◄── new access_token ───────── │  → Cấp access_token mới
```

### API Auth có sẵn

| Method | Endpoint | Mô tả | Auth? |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Đăng nhập | ❌ |
| `GET` | `/api/v1/auth/account` | Lấy thông tin tài khoản | ✅ |
| `GET` | `/api/v1/auth/refresh` | Làm mới access token | ❌ (dùng cookie) |
| `POST` | `/api/v1/auth/logout` | Đăng xuất | ✅ |

### Ví dụ gọi API đăng nhập

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin@sapo.vn", "password": "123456"}'
```

Response:
```json
{
  "statusCode": 200,
  "error": null,
  "message": "Đăng nhập thành công",
  "data": {
    "access_token": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 1,
      "email": "admin@sapo.vn",
      "name": "Admin",
      "role": { "id": 1, "name": "ADMIN" }
    }
  }
}
```

### Gọi API có bảo mật

```bash
curl -X GET http://localhost:8080/api/v1/xxx \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

---

## Cách Viết Code Mới (Hướng Dẫn Cho Nhóm)

### 1. Tạo Entity mới

Tạo file trong `domain/entity/`, kế thừa `BaseEntity`:

```java
@Entity
@Table(name = "ten_bang")
@Getter @Setter
public class TenEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên không được để trống")
    private String name;

    // ... các field khác
}
```

> `BaseEntity` tự động thêm: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`.

### 2. Tạo Repository

Tạo file trong `repository/`:

```java
@Repository
public interface TenEntityRepository
    extends JpaRepository<TenEntity, Long>, JpaSpecificationExecutor<TenEntity> {
    // Thêm custom query nếu cần
}
```

### 3. Tạo Service

Tạo file trong `service/`:

```java
@Service
public class TenEntityService {

    private final TenEntityRepository tenEntityRepository;

    public TenEntityService(TenEntityRepository tenEntityRepository) {
        this.tenEntityRepository = tenEntityRepository;
    }

    /**
     * Lấy danh sách có phân trang.
     *
     * @param pageable thông tin phân trang (page, size, sort)
     * @return ResultPaginationDTO chứa data + meta phân trang
     */
    public ResultPaginationDTO getAll(Pageable pageable) {
        // ... xử lý
    }
}
```

### 4. Tạo Controller

Tạo file trong `controller/`:

```java
@RestController
@RequestMapping("/api/v1/ten-entity")
@Tag(name = "Tên Module")
public class TenEntityController {

    private final TenEntityService tenEntityService;

    // Inject qua constructor

    @GetMapping(".json")
    @ApiMessage("Lấy danh sách thành công")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResultPaginationDTO> getAll(Pageable pageable) {
        return ResponseEntity.ok(tenEntityService.getAll(pageable));
    }

    @PostMapping(".json")
    @ApiMessage("Tạo mới thành công")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TenEntity> create(@Valid @RequestBody TenEntity entity) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenEntityService.create(entity));
    }
}
```

### 5. Thêm Permission cho API mới

Sau khi tạo API, thêm record vào bảng `permissions`:

```sql
INSERT INTO permissions (name, api_path, method, module)
VALUES ('Xem danh sách sản phẩm', '/api/v1/products', 'GET', 'PRODUCTS');
```

Sau đó gán permission đó vào role tương ứng trong bảng `permission_role`.

---

## Response Format Chuẩn

**Mọi API đều tự động trả về format này** (nhờ `FormatRestResponse`):

### Thành công
```json
{
  "statusCode": 200,
  "error": null,
  "message": "Lấy danh sách thành công",
  "data": { ... }
}
```

### Lỗi validation
```json
{
  "statusCode": 400,
  "error": "Dữ liệu đầu vào không hợp lệ",
  "message": ["name: Tên không được để trống", "price: Giá phải lớn hơn 0"],
  "data": null
}
```

### Lỗi không có quyền
```json
{
  "statusCode": 403,
  "error": "Không có quyền truy cập",
  "message": "Bạn không có quyền thực hiện thao tác này",
  "data": null
}
```

---

## Quy Tắc Coding (Bắt Buộc)

1. **Tên biến, method bằng tiếng Anh**, có nghĩa rõ ràng
2. **Mọi method trong service phải có JavaDoc** (`@param`, `@return`, mục đích)
3. **Validate trên server** — không tin dữ liệu từ client, dùng `@Valid` + `@NotBlank`, `@Min`...
4. **API path theo convention**: `GET /api/v1/products.json`, `POST /api/v1/orders.json`
5. **Chia nhỏ method** — không viết method quá dài, tách helper functions
6. **Dùng `@Transactional`** khi thao tác nhiều bảng cùng lúc (VD: tạo đơn hàng + trừ kho)
7. **Message lỗi rõ ràng** — dùng `@ApiMessage` và throw exception với message cụ thể
