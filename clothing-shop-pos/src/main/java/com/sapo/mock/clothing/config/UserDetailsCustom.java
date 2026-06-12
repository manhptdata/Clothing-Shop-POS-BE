package com.sapo.mock.clothing.config;

import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.user.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Tích hợp với Spring Security để load UserDetails từ database theo username.
 * Được dùng trong quá trình xác thực username/password khi đăng nhập.
 */
@Component("userDetailsService")
public class UserDetailsCustom implements UserDetailsService {

    private final UserService userService;

    public UserDetailsCustom(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User systemUser = userService.getUserByUsername(username);
        if (systemUser == null) {
            throw new UsernameNotFoundException("Không tìm thấy tài khoản: " + username);
        }
        if (!systemUser.isActive()) {
            throw new UsernameNotFoundException("Tài khoản đã bị khóa: " + username);
        }
        return new org.springframework.security.core.userdetails.User(
            systemUser.getUsername(),
            systemUser.getPasswordHash(),
            Collections.singletonList(new SimpleGrantedAuthority(systemUser.getRole().name()))
        );
    }
}
