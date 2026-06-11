package com.sapo.mock.clothing.util;

import com.sapo.mock.clothing.domain.response.ResLoginDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import org.springframework.security.oauth2.jwt.JwsHeader;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Utility class xử lý JWT: tạo access token, refresh token và lấy thông tin user hiện tại.
 */
@Service
public class SecurityUtil {

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    @Value("${hoangmelinh.jwt.access-token-validity-in-seconds}")
    private long accessTokenValidityInSeconds;

    @Value("${hoangmelinh.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidityInSeconds;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public SecurityUtil(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * Kiểm tra tính hợp lệ của refresh token — decode và trả về Jwt object.
     *
     * @param refreshToken chuỗi refresh token cần kiểm tra
     * @return Jwt object đã decode nếu token hợp lệ
     * @throws org.springframework.security.oauth2.jwt.JwtException nếu token không hợp lệ hoặc hết hạn
     */
    public Jwt checkValidRefreshToken(String refreshToken) {
        return this.jwtDecoder.decode(refreshToken);
    }

    /**
     * Tạo JWT access token sau khi đăng nhập thành công.
     *
     * @param email    email của người dùng (subject của token)
     * @param dto      đối tượng chứa thông tin user để nhúng vào claims
     * @return chuỗi JWT access token có thời hạn 24 giờ
     */
    public String createAccessToken(String email, ResLoginDTO dto) {
        ResLoginDTO.UserInsideToken userInsideToken = buildUserInsideToken(dto);

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenValidityInSeconds, ChronoUnit.SECONDS))
            .subject(email)
            .claim("user", userInsideToken)
            .build();

        return encodeJwt(claims);
    }

    /**
     * Tạo JWT refresh token có thời hạn dài hơn access token (7 ngày).
     *
     * @param email    email của người dùng
     * @param dto      đối tượng chứa thông tin user
     * @return chuỗi JWT refresh token
     */
    public String createRefreshToken(String email, ResLoginDTO dto) {
        ResLoginDTO.UserInsideToken userInsideToken = buildUserInsideToken(dto);

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plus(refreshTokenValidityInSeconds, ChronoUnit.SECONDS))
            .subject(email)
            .claim("user", userInsideToken)
            .build();

        return encodeJwt(claims);
    }

    /**
     * Lấy email của người dùng đang đăng nhập từ SecurityContext.
     *
     * @return Optional chứa email, hoặc empty nếu chưa xác thực
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractEmailFromAuthentication(securityContext.getAuthentication()));
    }

    private static String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }

    private ResLoginDTO.UserInsideToken buildUserInsideToken(ResLoginDTO dto) {
        ResLoginDTO.UserInsideToken userInsideToken = new ResLoginDTO.UserInsideToken();
        userInsideToken.setId(dto.getUser().getId());
        userInsideToken.setEmail(dto.getUser().getEmail());
        userInsideToken.setName(dto.getUser().getName());
        return userInsideToken;
    }

    private String encodeJwt(JwtClaimsSet claims) {
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}
