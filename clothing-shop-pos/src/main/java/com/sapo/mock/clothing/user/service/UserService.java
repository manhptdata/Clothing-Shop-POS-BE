package com.sapo.mock.clothing.user.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
	@Cacheable(value = "users", key = "#username")
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
	@CacheEvict(value = "users", key = "#username")
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

	/**
	 * Tạo mã PIN bảo mật ngẫu nhiên (6 số) cho user và mã hoá trước khi lưu.
	 *
	 * @param username Tên đăng nhập của user
	 * @return Mã PIN dạng plain text (để trả về cho frontend hiển thị 1 lần)
	 */
	@CacheEvict(value = "users", key = "#username")
	public String generateSecurityPin(String username) {
		User user = userRepository.findByUsername(username);
		if (user == null) {
			throw new RuntimeException("User không tồn tại");
		}
		
		// Generate random 6-digit PIN
		int pin = (int) (Math.random() * 900000) + 100000;
		String plainPin = String.valueOf(pin);
		
		user.setSecurityPin(plainPin); // Luu plaintext
		userRepository.save(user);
		
		return plainPin;
	}

	@CacheEvict(value = "users", key = "#username")
	public void changeSecurityPin(String username, String newPin) {
		User user = userRepository.findByUsername(username);
		if (user == null) {
			throw new RuntimeException("User không tồn tại");
		}
		if (newPin == null || !newPin.matches("\\d{6}")) {
			throw new RuntimeException("Mã PIN phải bao gồm 6 chữ số");
		}
		user.setSecurityPin(newPin); // Luu plaintext
		userRepository.save(user);
	}

	public String getSecurityPin(String username) {
		User user = userRepository.findByUsername(username);
		if (user == null) {
			throw new RuntimeException("User không tồn tại");
		}
		return user.getSecurityPin();
	}

	@CacheEvict(value = "users", key = "#username")
	public void changePassword(String username, String oldPassword, String newPassword) {
		User user = userRepository.findByUsername(username);
		if (user == null) {
			throw new RuntimeException("User không tồn tại");
		}
		if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
			throw new RuntimeException("Mật khẩu cũ không chính xác");
		}
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}

	@CacheEvict(value = "users", allEntries = true)
	public User updateUserProfileByAdmin(Integer userId, com.sapo.mock.clothing.user.dto.request.UpdateProfileRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User không tồn tại"));

		user.setFullName(request.getFullName());
		user.setPhone(request.getPhone());
		user.setEmail(request.getEmail());

		return userRepository.save(user);
	}

}
