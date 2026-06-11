package com.sapo.mock.clothing.controller;

import com.sapo.mock.clothing.domain.entity.User;
import com.sapo.mock.clothing.domain.request.ReqLoginDTO;
import com.sapo.mock.clothing.domain.response.ResLoginDTO;
import com.sapo.mock.clothing.service.UserService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import com.sapo.mock.clothing.util.error.IdInvalidException;
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

@RestController
@RequestMapping("/api/v1/auth")
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

    /**
     * Đăng nhập hệ thống — trả về access token và set refresh token vào cookie.
     *
     * @param loginRequest DTO chứa username (email) và password
     * @return ResLoginDTO chứa access_token và thông tin user
     */
    @PostMapping("/login")
    @ApiMessage("Đăng nhập thành công")
    @Operation(summary = "Đăng nhập", description = "Xác thực email + password, nhận JWT access token")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginRequest) {
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResLoginDTO responseDTO = new ResLoginDTO();
        User currentUser = userService.getUserByEmail(loginRequest.getUsername());

        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
            currentUser.getId(),
            currentUser.getEmail(),
            currentUser.getName(),
            currentUser.getRole()
        );
        responseDTO.setUser(userLogin);

        String accessToken = securityUtil.createAccessToken(loginRequest.getUsername(), responseDTO);
        responseDTO.setAccessToken(accessToken);

        String refreshToken = securityUtil.createRefreshToken(loginRequest.getUsername(), responseDTO);
        userService.updateRefreshToken(refreshToken, loginRequest.getUsername());

        ResponseCookie refreshTokenCookie = ResponseCookie
            .from("refresh_token", refreshToken)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(responseDTO);
    }

    /**
     * Lấy thông tin tài khoản của người dùng đang đăng nhập.
     *
     * @return thông tin user từ token
     */
    @GetMapping("/account")
    @ApiMessage("Lấy thông tin tài khoản thành công")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại")
    public ResponseEntity<ResLoginDTO.UserGetAccount> getAccountInfo() {
        String currentEmail = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = userService.getUserByEmail(currentEmail);

        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
            currentUser.getId(),
            currentUser.getEmail(),
            currentUser.getName(),
            currentUser.getRole()
        );

        ResLoginDTO.UserGetAccount accountInfo = new ResLoginDTO.UserGetAccount(userLogin);
        return ResponseEntity.ok(accountInfo);
    }

    /**
     * Cấp mới access token dựa trên refresh token trong cookie.
     *
     * @param refreshTokenCookie refresh token lấy từ cookie
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

        Jwt decodedRefreshToken = securityUtil.checkValidRefreshToken(refreshTokenCookie);
        String emailFromToken = decodedRefreshToken.getSubject();

        User userFromDB = userService.getUserByRefreshTokenAndEmail(refreshTokenCookie, emailFromToken);
        if (userFromDB == null) {
            throw new IdInvalidException("Refresh token không hợp lệ. Vui lòng đăng nhập lại.");
        }

        ResLoginDTO responseDTO = new ResLoginDTO();
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
            userFromDB.getId(),
            userFromDB.getEmail(),
            userFromDB.getName(),
            userFromDB.getRole()
        );
        responseDTO.setUser(userLogin);

        String newAccessToken = securityUtil.createAccessToken(emailFromToken, responseDTO);
        responseDTO.setAccessToken(newAccessToken);

        String newRefreshToken = securityUtil.createRefreshToken(emailFromToken, responseDTO);
        userService.updateRefreshToken(newRefreshToken, emailFromToken);

        ResponseCookie newRefreshCookie = ResponseCookie
            .from("refresh_token", newRefreshToken)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
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
        String currentEmail = SecurityUtil.getCurrentUserLogin().orElse("");
        if (!currentEmail.isEmpty()) {
            userService.updateRefreshToken(null, currentEmail);
        }

        ResponseCookie clearCookie = ResponseCookie
            .from("refresh_token", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
            .build();
    }
}
