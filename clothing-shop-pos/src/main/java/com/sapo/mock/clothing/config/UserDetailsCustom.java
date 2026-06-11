package com.sapo.mock.clothing.config;

import com.sapo.mock.clothing.service.UserService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Tích hợp với Spring Security để load UserDetails từ database theo email.
 * Được dùng trong quá trình xác thực username/password khi đăng nhập.
 */
@Component("userDetailsService")
public class UserDetailsCustom implements UserDetailsService {

    private final UserService userService;

    public UserDetailsCustom(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.sapo.mock.clothing.domain.entity.User systemUser = userService.getUserByEmail(email);
        if (systemUser == null) {
            throw new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email);
        }
        return User.withUsername(systemUser.getEmail())
            .password(systemUser.getPassword())
            .authorities("ROLE_USER")
            .build();
    }
}
