# CRM Customer – Tài liệu API

> Base URL: `http://localhost:8080/api/v1/crm`

---

## 1. Khách Hàng (Customer)

**Controller:** `CustomerController`  
**Base path:** `/customers`

### 1.1 Tìm kiếm khách hàng

```
GET /api/v1/crm/customers/search?keyword=&page=0&size=10
```

- Tìm theo **tên** hoặc **số điện thoại** (không phân biệt hoa thường)
- Chỉ trả về khách hàng có status **ACTIVE**
- Kết quả sắp xếp theo `createdAt` giảm dần

**Query params:**

| Param | Default | Mô tả |
|---|---|---|
| `keyword` | `""` | Từ khóa tìm kiếm |
| `page` | `0` | Trang hiện tại |
| `size` | `10` | Số bản ghi mỗi trang |

---

### 1.2 Xem chi tiết khách hàng

```
GET /api/v1/crm/customers/{id}
```

---

### 1.3 Thêm mới khách hàng

```
POST /api/v1/crm/customers
```

**Body (JSON):**
```json
{
  "fullName": "Nguyễn Văn A",
  "phone": "0901234567",
  "dateOfBirth": "1999-05-20",
  "gender": "MALE",
  "address": "123 Lê Lợi, TP.HCM",
  "note": "Khách VIP"
}
```

- Kiểm tra trùng số điện thoại trước khi lưu
- Trả về HTTP **201 Created**

---

### 1.4 Cập nhật thông tin khách hàng

```
PUT /api/v1/crm/customers/{id}
```

**Body:** Tương tự tạo mới, thêm trường `status` (ACTIVE / INACTIVE)

---

### 1.5 Khóa tài khoản khách hàng (xóa mềm)

```
PATCH /api/v1/crm/customers/{id}/deactivate
```

- Chuyển `status` → `INACTIVE`, **không xóa khỏi DB**

---

### 1.6 Mở khóa tài khoản khách hàng

```
PATCH /api/v1/crm/customers/{id}/activate
```

- Chuyển `status` → `ACTIVE`

---

## 2. Nhóm Khách Hàng (Customer Group)

**Controller:** `CustomerGroupController`  
**Base path:** `/customer-groups`

### 2.1 Danh sách nhóm có phân trang

```
GET /api/v1/crm/customer-groups?page=0&size=10
```

- Sắp xếp theo `id` tăng dần
- **Lưu ý:** `totalCustomers` sẽ là `null` ở endpoint này (dùng `/search` nếu cần đếm thành viên)

---

### 2.2 Tìm kiếm nhóm khách hàng

```
GET /api/v1/crm/customer-groups/search?keyword=khu vực&page=0&size=10
```

- Chỉ trả về nhóm có status **ACTIVE**
- Tìm kiếm theo **tên nhóm** (không phân biệt hoa thường)
- Kèm theo `totalCustomers` — số lượng thành viên trong nhóm (tính bằng `COUNT` từ JPQL)
- Sắp xếp theo `createdAt` giảm dần

---

### 2.3 Xem chi tiết một nhóm

```
GET /api/v1/crm/customer-groups/{id}
```

- Trả về thông tin nhóm kèm `totalCustomers`

---

### 2.4 Tạo mới nhóm khách hàng

```
POST /api/v1/crm/customer-groups
```

**Body (JSON):**
```json
{
  "name": "Khách Khu Vực A",
  "description": "Nhóm khách hàng ở khu vực A",
  "note": "Ưu tiên giao hàng"
}
```

- `status` mặc định là `ACTIVE`
- Trả về HTTP **201 Created**

---

### 2.5 Gán khách hàng vào nhóm (kèm cập nhật thông tin)

```
PUT /api/v1/crm/customer-groups/{customerId}/assign
```
**Body (JSON):** Gửi đầy đủ thông tin khách hàng + `customerGroupId`

```json
{
  "fullName": "Nguyễn Văn A",
  "phone": "0901234567",
  "dateOfBirth": "1999-05-20",
  "gender": "MALE",
  "address": "123 Lê Lợi",
  "note": "",
  "status": "ACTIVE",
  "customerGroupId": 5
}
```

- Truyền `customerGroupId: null` để **rút khách ra khỏi nhóm**
- Đồng thời **cập nhật toàn bộ thông tin** khách hàng

---

### 2.6 Chỉ gán / đổi / rút nhóm nhanh


```
PATCH /api/v1/crm/customer-groups/{customerId}/assign-group
```

**Body (JSON):** Chỉ cần gửi mỗi `customerGroupId`

```json
{ "customerGroupId": 5 }
```

Rút khỏi nhóm:
```json
{ "customerGroupId": null }
```

- **Không cần** gửi lại thông tin khách hàng
- Dùng khi chỉ muốn gán/đổi/rút nhóm nhanh

---

### 2.7 Xem danh sách thành viên của một nhóm

```
GET /api/v1/crm/customer-groups/{groupId}/members?page=0&size=10
```

- Chỉ trả về khách hàng có status **ACTIVE**
- Gọi qua `CustomerService.getCustomersByGroupId`

---

## 3. Cấu trúc Response chung

Tất cả API đều bọc trong `RestResponse<T>`:

```json
{
  "statusCode": 200,
  "error": null,
  "message": "...",
  "data": { ... }
}
```

---

## 4. Cấu trúc Package

```
crm/
├── controller/
│   ├── CustomerController.java
│   └── CustomerGroupController.java
├── service/
│   ├── CustomerService.java (interface)
│   ├── CustomerGroupService.java (interface)
│   └── impl/
│       ├── CustomerServiceImpl.java
│       └── CustomerGroupServiceImpl.java
├── repository/
│   ├── CustomerRepository.java
│   └── CustomerGroupRepository.java
└── dto/
    ├── request/
    │   ├── customer/
    │   │   ├── CustomerCreateRequest.java
    │   │   └── CustomerUpdateRequest.java
    │   └── groupcustomer/
    │       ├── CustomerGroupRequest.java
    │       ├── CustomerRequest.java
    │       └── AssignGroupRequest.java
    └── response/
        ├── CustomerResponse.java       ← có inner class GroupInfo
        └── CustomerGroupResponse.java  ← có field totalCustomers
```

---

## 5. Ghi chú

- `totalCustomers` được **tính toán tại query time** bằng `COUNT(c)` + `GROUP BY`, không lưu vào DB
- Xóa khách hàng là **xóa mềm** (soft delete) — chuyển `status = INACTIVE`
- `CustomerResponse` có inner class `GroupInfo { id, name }` để hiển thị thông tin nhóm rút gọn
