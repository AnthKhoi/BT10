# HƯỚNG DẪN BÀI TẬP VÍ DỤ: JSON WEB TOKEN (JWT) VỚI SPRING BOOT 3 & SPRING SECURITY 6

> **Môn học:** Lập trình Web (WEBPR330479)  
> **Giảng viên:** ThS. Nguyễn Hữu Trung - FIT HCMUTE  
> **Đề tài:** Demo JWT với Spring Boot 3 – Security 6  

---

## 1. Cấu trúc thư mục dự án

```
BAITAP10.1/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/vn/iotstar/
│   │   │   ├── JwtSpringboot3Application.java
│   │   │   ├── configs/
│   │   │   │   ├── ApplicationConfiguration.java
│   │   │   │   ├── SecurityConfiguration.java
│   │   │   │   └── DataInitializer.java
│   │   │   ├── controllers/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── AuthenticationController.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── UserController.java
│   │   │   ├── entity/
│   │   │   │   └── User.java
│   │   │   ├── filter/
│   │   │   │   └── JwtAuthenticationFilter.java
│   │   │   ├── models/
│   │   │   │   ├── LoginResponse.java
│   │   │   │   ├── LoginUserModel.java
│   │   │   │   └── RegisterUserModel.java
│   │   │   ├── repository/
│   │   │   │   └── UserRepository.java
│   │   │   └── services/
│   │   │       ├── AuthenticationService.java
│   │   │       ├── JwtService.java
│   │   │       └── UserService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/
│   │       │   ├── images/u1.jpg
│   │       │   └── js/mainjs.js
│   │       └── templates/
│   │           ├── login.html
│   │           └── profile.html
│   └── test/java/vn/iotstar/
│       └── JwtSpringboot3ApplicationTests.java
```

---

## 2. Chi tiết 10 bước thực hiện theo bài giảng

### Bước 1: Thêm Dependency (Slide 15)
- Sử dụng thư viện **JJWT 0.12.6**:
  - `io.jsonwebtoken:jjwt-api:0.12.6`
  - `io.jsonwebtoken:jjwt-impl:0.12.6`
  - `io.jsonwebtoken:jjwt-jackson:0.12.6`
- Spring Boot Starter Web, Security, Data JPA, Thymeleaf.
- Hỗ trợ cả **H2 In-Memory Database** (chạy ngay) và **MySQL / SQL Server**.

### Bước 2: Tạo Entity `User` (Slide 16)
- Implements `UserDetails` của Spring Security.
- Các trường: `id`, `fullName`, `email` (unique), `password`, `images`, `createdAt`, `updatedAt`.
- Triển khai các phương thức xác thực: `getAuthorities()`, `getUsername()`, `isAccountNonLocked()`, v.v.

### Bước 3: Tạo Models DTO (Slide 17)
- `LoginResponse.java`: chứa chuỗi `token` và `expiresIn`.
- `LoginUserModel.java`: `email`, `password`.
- `RegisterUserModel.java`: `email`, `password`, `fullName`.

### Bước 4: Tạo Repository & Services (Slide 18 - 21)
- `UserRepository.java`: kế thừa `JpaRepository<User, Integer>`, tìm kiếm bằng `findByEmail(String email)`.
- `UserService.java`: hàm `allUsers()` lấy toàn bộ danh sách người dùng.
- `AuthenticationService.java`:
  - `signup()`: mã hóa mật khẩu bằng BCryptPasswordEncoder và lưu User.
  - `authenticate()`: gọi `authenticationManager.authenticate(...)` để kiểm tra tài khoản.
- `JwtService.java`:
  - Ký token với thuật toán HMAC-SHA256 (`Jwts.SIG.HS256`).
  - Giải mã và trích xuất Claims, Username, hạn dùng `expiration`.
  - Kiểm tra token hợp lệ `isTokenValid()`.
- `application.properties`: Cổng `8005`, bí mật khóa HMAC `security.jwt.secret-key`, thời hạn `security.jwt.expiration-time=3600000` (1 giờ).

### Bước 5: Cấu hình `ApplicationConfiguration` (Slide 22)
- Bean `UserDetailsService`: load thông tin qua email.
- Bean `BCryptPasswordEncoder`: mã hóa mật khẩu an toàn.
- Bean `AuthenticationManager`: quản lý xác thực.
- Bean `AuthenticationProvider`: `DaoAuthenticationProvider`.

### Bước 6: Tạo `JwtAuthenticationFilter` (Slide 23)
- Kế thừa `OncePerRequestFilter`.
- Bắt header `Authorization: Bearer <token>`.
- Trích xuất email, nạp UserDetails và lưu vào `SecurityContextHolder`.

### Bước 7: Cấu hình `SecurityConfiguration` (Slide 24, 25)
- Tắt CSRF (`csrf.disable()`).
- Session vô trạng thái (`SessionCreationPolicy.STATELESS`).
- Mở quyền truy cập `permitAll()` cho: `/auth/**`, `/login/**`, `/user/**`, `/images/**`, `/js/**`.
- Các yêu cầu còn lại bắt buộc có JWT token (`authenticated()`).
- Đặt `JwtAuthenticationFilter` trước `UsernamePasswordAuthenticationFilter`.
- Cấu hình CORS cho phép `http://localhost:8005`.

### Bước 8: Tạo các Controller (Slide 26, 27)
- `AuthenticationController` (`/auth`):
  - `POST /auth/signup`: Đăng ký tài khoản mới.
  - `POST /auth/login`: Đăng nhập, trả về chuỗi JWT token.
- `UserController` (`/users`):
  - `GET /users/me`: Trả về thông tin người dùng đang đăng nhập từ `SecurityContextHolder`.
  - `GET /users/`: Trả về danh sách tất cả người dùng (yêu cầu JWT).

### Bước 9: Kiểm thử qua REST API / Postman (Slide 28, 29)
1. Gửi request `POST http://localhost:8005/auth/signup`
2. Gửi request `POST http://localhost:8005/auth/login` -> nhận về chuỗi JWT `token`.
3. Gửi request `GET http://localhost:8005/users/me` kèm header:
   ```
   Authorization: Bearer <token_vừa_nhận>
   ```

### Bước 10: Xử lý Ngoại lệ Global Exception (Slide 30, 31)
- `GlobalExceptionHandler`:
  - `BadCredentialsException` -> `401 Unauthorized` ("The username or password is incorrect").
  - `SignatureException` -> `403 Forbidden` / `401` ("The JWT signature is invalid").
  - `ExpiredJwtException` -> `403 Forbidden` / `401` ("The JWT token has expired").
  - `AccessDeniedException` -> `403 Forbidden`.

### Bước 10 (tiếp theo): Giao diện AJAX & Thymeleaf (Slide 32 - 34)
- `AuthController.java`:
  - `GET /login` -> `login.html`
  - `GET /user/profile` -> `profile.html`
- `mainjs.js`: Sử dụng jQuery AJAX:
  - Khi click `#login`: Gửi AJAX `POST /auth/login`, nhận JWT lưu vào `localStorage.token` và chuyển sang `/user/profile`.
  - Tại trang `/user/profile`: Gửi AJAX `GET /users/me` kèm `Authorization: Bearer + localStorage.token` và hiển thị thông tin lên giao diện.
  - Khi click `#logout`: Xóa `localStorage.clear()` và trở về `/login`.

---

## 3. Cách chạy ứng dụng

### Chạy từ Terminal / Command Prompt:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
mvn spring-boot:run
```

### Truy cập trình duyệt:
- **Trang đăng nhập:** [http://localhost:8005/login](http://localhost:8005/login)
  - Tài khoản mẫu khởi tạo sẵn: `trungnh@hcmute.edu.vn`
  - Mật khẩu: `123456`
- **Trang thông tin cá nhân:** [http://localhost:8005/user/profile](http://localhost:8005/user/profile)
- **H2 Console:** [http://localhost:8005/h2-console](http://localhost:8005/h2-console) (JDBC URL: `jdbc:h2:mem:jwt_springboot3`, User: `sa`, Password để trống).
