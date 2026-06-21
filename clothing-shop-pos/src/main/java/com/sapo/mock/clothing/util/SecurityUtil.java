package com.sapo.mock.clothing.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.sapo.mock.clothing.auth.dto.response.ResLoginDTO;

/**
 * Utility class xử lý JWT: tạo access token, refresh token và lấy thông tin
 * user hiện tại.
 */
@Service
public class SecurityUtil {

	public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

	@Value("${sapo.jwt.access-token-validity-in-seconds}")
	private long accessTokenValidityInSeconds;

	@Value("${sapo.jwt.refresh-token-validity-in-seconds}")
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
	 * @throws org.springframework.security.oauth2.jwt.JwtException nếu token không
	 *                                                              hợp lệ hoặc hết
	 *                                                              hạn
	 */
	public Jwt checkValidRefreshToken(String refreshToken) {
		return this.jwtDecoder.decode(refreshToken);
	}

	/**
	 * Tạo JWT access token sau khi đăng nhập thành công.
	 *
	 * @param username username của người dùng (subject của token)
	 * @param dto      đối tượng chứa thông tin user để nhúng vào claims
	 * @return chuỗi JWT access token có thời hạn 24 giờ
	 */
	public String createAccessToken(String username, ResLoginDTO dto) {
		ResLoginDTO.UserInsideToken userInsideToken = buildUserInsideToken(dto);

		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuedAt(now)
				.expiresAt(now.plus(accessTokenValidityInSeconds, ChronoUnit.SECONDS)).subject(username)
				.claim("user", userInsideToken)
				.claim("roles", dto.getUser().getRole())
				.build();

		return encodeJwt(claims);
	}

	/**
	 * Tạo JWT refresh token có thời hạn dài hơn access token (7 ngày).
	 *
	 * @param username username của người dùng
	 * @param dto      đối tượng chứa thông tin user
	 * @return chuỗi JWT refresh token
	 */
	public String createRefreshToken(String username, ResLoginDTO dto) {
		ResLoginDTO.UserInsideToken userInsideToken = buildUserInsideToken(dto);

		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuedAt(now)
				.expiresAt(now.plus(refreshTokenValidityInSeconds, ChronoUnit.SECONDS)).subject(username)
				.claim("user", userInsideToken)
				.claim("roles", dto.getUser().getRole())
				.build();

		return encodeJwt(claims);
	}

	/**
	 * Lấy username của người dùng đang đăng nhập từ SecurityContext.
	 *
	 * @return Optional chứa username, hoặc empty nếu chưa xác thực
	 */
	public static Optional<String> getCurrentUserLogin() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		return Optional.ofNullable(extractUsernameFromAuthentication(securityContext.getAuthentication()));
	}

	private static String extractUsernameFromAuthentication(Authentication authentication) {
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
		userInsideToken.setUsername(dto.getUser().getUsername());
		userInsideToken.setFullName(dto.getUser().getFullName());
		return userInsideToken;
	}

	private String encodeJwt(JwtClaimsSet claims) {
		JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
		return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
	}

	public static Integer getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof Jwt) {
			Jwt jwt = (Jwt) auth.getPrincipal();

			// Lấy object "user" đã lưu trong Token (được parse thành Map)
			Map<String, Object> userClaim = jwt.getClaim("user");

			if (userClaim != null && userClaim.containsKey("id")) {
				// Vì JSON có thể parse số nguyên thành Long hoặc Integer tùy ý,
				// nên ép về chuỗi rồi parse ngược lại Integer cho an toàn nhất
				return Integer.valueOf(userClaim.get("id").toString());
			}
		}
		return null;
	}
}
