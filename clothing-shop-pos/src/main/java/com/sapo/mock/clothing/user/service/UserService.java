package com.sapo.mock.clothing.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.user.repository.UserRepository;

/**
 * Service xử lý các nghiệp vụ liên quan đến User: tìm kiếm, tạo mới, quản lý
 * refresh token.
 */
@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Tìm user theo username.
	 *
	 * @param username tên đăng nhập cần tìm
	 * @return User nếu tìm thấy, null nếu không có
	 */
	public User getUserByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	/**
	 * Tạo user mới — mã hoá password trước khi lưu.
	 *
	 * @param newUser đối tượng User cần tạo (password dạng plain text)
	 * @return User đã được lưu vào DB (password đã hash)
	 */
	public User createUser(User newUser) {
		newUser.setPasswordHash(passwordEncoder.encode(newUser.getPasswordHash()));
		return userRepository.save(newUser);
	}

	/**
	 * Cập nhật refresh token cho user (dùng sau khi đăng nhập hoặc refresh).
	 *
	 * @param refreshToken chuỗi refresh token mới, hoặc null khi logout
	 * @param username     username của user cần cập nhật
	 */
	public void updateRefreshToken(String refreshToken, String username) {
		User user = userRepository.findByUsername(username);
		if (user != null) {
			user.setRefreshToken(refreshToken);
			userRepository.save(user);
		}
	}

	/**
	 * Tìm user theo refresh token + username — dùng để validate khi refresh access
	 * token.
	 *
	 * @param refreshToken chuỗi refresh token
	 * @param username     username tương ứng
	 * @return User nếu hợp lệ, null nếu không tìm thấy
	 */
	public User getUserByRefreshTokenAndUsername(String refreshToken, String username) {
		return userRepository.findByRefreshTokenAndUsername(refreshToken, username);
	}

}
