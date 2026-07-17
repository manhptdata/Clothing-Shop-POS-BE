package com.sapo.mock.clothing.auth.controller;

import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.auth.dto.request.ReqLoginDTO;
import com.sapo.mock.clothing.auth.dto.response.ResLoginDTO;
import com.sapo.mock.clothing.user.service.UserService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import com.sapo.mock.clothing.exception.IdInvalidException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý xác thực: đăng nhập, đăng xuất, refresh token, lấy thông tin
 * tài khoản.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Đăng nhập, đăng xuất, refresh token")
public class AuthController {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder,
            SecurityUtil securityUtil,
            UserService userService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
    }

    // trigger cicd
    //
    /**
     * Đăng nhập hệ thống.
     *
     * @param loginRequest DTO chứa username và password
     * @return ResLoginDTO chứa access_token và thông tin user
     */
    @PostMapping("/login")
    @ApiMessage("Đăng nhập thành công")
    @Operation(summary = "Đăng nhập", description = "Xác thực username + password, nhận JWT access token")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginRequest) {
        // 1. Xác thực với Spring Security
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), loginRequest.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Build response
        User currentUser = userService.getUserByUsername(loginRequest.getUsername());
        ResLoginDTO responseDTO = buildLoginResponse(currentUser);

        // 3. Tạo access token
        String accessToken = securityUtil.createAccessToken(currentUser.getUsername(), responseDTO);
        responseDTO.setAccessToken(accessToken);

        // 4. Tạo refresh token + lưu DB + set cookie
        String refreshToken = securityUtil.createRefreshToken(currentUser.getUsername(), responseDTO);
        userService.updateRefreshToken(refreshToken, currentUser.getUsername());

        ResponseCookie refreshTokenCookie = buildRefreshTokenCookie(refreshToken, 7 * 24 * 60 * 60);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(responseDTO);
    }

    /**
     * Lấy thông tin tài khoản của người dùng đang đăng nhập.
     *
     * @return thông tin user hiện tại
     */
    @GetMapping("/account")
    @ApiMessage("Lấy thông tin tài khoản thành công")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại")
    public ResponseEntity<ResLoginDTO.UserGetAccount> getAccountInfo() {
        String currentUsername = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = userService.getUserByUsername(currentUsername);

        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : "ROLE_UNKNOWN";
        java.util.List<String> perms = new java.util.ArrayList<>();
        if (currentUser.getRole() != null && currentUser.getRole().getPermissions() != null) {
            perms = currentUser.getRole().getPermissions().stream().map(Enum::name).collect(java.util.stream.Collectors.toList());
        }

        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getFullName(),
                currentUser.getPhone(),
                roleName,
                perms);
        return ResponseEntity.ok(new ResLoginDTO.UserGetAccount(userLogin));
    }

    /**
     * Cấp mới access token dựa trên refresh token trong cookie.
     *
     * @param refreshTokenCookie refresh token từ cookie
     * @return ResLoginDTO với access token mới
     */
    @GetMapping("/refresh")
    @ApiMessage("Làm mới token thành công")
    @Operation(summary = "Refresh access token bằng refresh token cookie")
    public ResponseEntity<ResLoginDTO> refreshAccessToken(
            @CookieValue(name = "refresh_token", defaultValue = "") String refreshTokenCookie)
            throws IdInvalidException {

        if (refreshTokenCookie.isEmpty()) {
            throw new IdInvalidException("Refresh token không tồn tại. Vui lòng đăng nhập lại.");
        }

        Jwt decodedToken = securityUtil.checkValidRefreshToken(refreshTokenCookie);
        String usernameFromToken = decodedToken.getSubject();

        User userFromDB = userService.getUserByRefreshTokenAndUsername(refreshTokenCookie, usernameFromToken);
        if (userFromDB == null) {
            throw new IdInvalidException("Refresh token không hợp lệ. Vui lòng đăng nhập lại.");
        }

        ResLoginDTO responseDTO = buildLoginResponse(userFromDB);

        String newAccessToken = securityUtil.createAccessToken(usernameFromToken, responseDTO);
        responseDTO.setAccessToken(newAccessToken);

        String newRefreshToken = securityUtil.createRefreshToken(usernameFromToken, responseDTO);
        userService.updateRefreshToken(newRefreshToken, usernameFromToken);

        ResponseCookie newCookie = buildRefreshTokenCookie(newRefreshToken, 7 * 24 * 60 * 60);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newCookie.toString())
                .body(responseDTO);
    }

    /**
     * Đăng xuất — xoá refresh token trong DB và clear cookie.
     *
     * @return 200 OK
     */
    @PostMapping("/logout")
    @ApiMessage("Đăng xuất thành công")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Đăng xuất")
    public ResponseEntity<Void> logout() {
        String currentUsername = SecurityUtil.getCurrentUserLogin().orElse("");
        if (!currentUsername.isEmpty()) {
            userService.updateRefreshToken(null, currentUsername);
        }
        ResponseCookie clearCookie = buildRefreshTokenCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    // ──── Helper methods ────

    private ResLoginDTO buildLoginResponse(User user) {
        ResLoginDTO dto = new ResLoginDTO();
        String roleName = user.getRole() != null ? user.getRole().getName() : "ROLE_UNKNOWN";
        java.util.List<String> perms = new java.util.ArrayList<>();
        if (user.getRole() != null && user.getRole().getPermissions() != null) {
            perms = user.getRole().getPermissions().stream().map(Enum::name).collect(java.util.stream.Collectors.toList());
        }
        dto.setUser(new ResLoginDTO.UserLogin(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getPhone(),
                roleName,
                perms));
        return dto;
    }

    private ResponseCookie buildRefreshTokenCookie(String value, long maxAge) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
