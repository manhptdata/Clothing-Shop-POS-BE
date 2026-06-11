package com.sapo.mock.clothing.service;

import com.sapo.mock.clothing.domain.entity.User;
import com.sapo.mock.clothing.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service xử lý các nghiệp vụ liên quan đến User: CRUD, xác thực, quản lý refresh token.
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
     * Tìm user theo email.
     *
     * @param email địa chỉ email cần tìm
     * @return User nếu tìm thấy, null nếu không có
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Kiểm tra email đã tồn tại trong hệ thống chưa.
     *
     * @param email địa chỉ email cần kiểm tra
     * @return true nếu email đã tồn tại
     */
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Tạo user mới — mã hoá password trước khi lưu.
     *
     * @param newUser đối tượng User cần tạo (password dạng plain text)
     * @return User đã được lưu vào DB (password đã hash)
     */
    public User createUser(User newUser) {
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        return userRepository.save(newUser);
    }

    /**
     * Cập nhật thông tin user.
     *
     * @param updatedUser đối tượng User với thông tin mới (không bao gồm password)
     * @return User sau khi cập nhật, null nếu không tìm thấy
     */
    public User updateUser(User updatedUser) {
        Optional<User> existingUser = userRepository.findById(updatedUser.getId());
        if (existingUser.isEmpty()) {
            return null;
        }
        User userToUpdate = existingUser.get();
        userToUpdate.setName(updatedUser.getName());
        userToUpdate.setAddress(updatedUser.getAddress());
        userToUpdate.setAge(updatedUser.getAge());
        userToUpdate.setGender(updatedUser.getGender());
        userToUpdate.setRole(updatedUser.getRole());
        return userRepository.save(userToUpdate);
    }

    /**
     * Lấy thông tin user theo ID.
     *
     * @param userId ID của user
     * @return User nếu tìm thấy, null nếu không
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * Cập nhật refresh token cho user (dùng sau khi đăng nhập hoặc refresh).
     *
     * @param refreshToken chuỗi refresh token mới, hoặc null khi logout
     * @param email        email của user cần cập nhật
     */
    public void updateRefreshToken(String refreshToken, String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setRefreshToken(refreshToken);
            userRepository.save(user);
        }
    }

    /**
     * Tìm user theo refresh token và email — dùng để validate khi refresh access token.
     *
     * @param refreshToken chuỗi refresh token cần tìm
     * @param email        email tương ứng
     * @return User nếu hợp lệ, null nếu không tìm thấy
     */
    public User getUserByRefreshTokenAndEmail(String refreshToken, String email) {
        return userRepository.findByRefreshTokenAndEmail(refreshToken, email);
    }
}
