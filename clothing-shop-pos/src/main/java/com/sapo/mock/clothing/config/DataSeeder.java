package com.sapo.mock.clothing.config;

import com.sapo.mock.clothing.entity.Role;
import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.user.repository.RoleRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.util.constant.PermissionEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info(">>> Checking and seeding Roles...");

        // 1. Create ADMIN role
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_ADMIN");
            role.setDescription("Quản lý toàn hệ thống");
            role.setSystem(true);
            role.setPermissions(new HashSet<>(Arrays.asList(PermissionEnum.values())));
            return roleRepository.save(role);
        });

        // 1.5 Create MANAGER role
        Role managerRole = roleRepository.findByName("ROLE_MANAGER").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_MANAGER");
            role.setDescription("Cửa hàng trưởng");
            role.setSystem(true);
            Set<PermissionEnum> permissions = new HashSet<>(Arrays.asList(PermissionEnum.values()));
            permissions.remove(PermissionEnum.MANAGE_USER);
            permissions.remove(PermissionEnum.MANAGE_ROLE);
            role.setPermissions(permissions);
            return roleRepository.save(role);
        });

        // 2. Create SALE role
        Role saleRole = roleRepository.findByName("ROLE_SALE").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_SALE");
            role.setDescription("Nhân viên Bán hàng");
            role.setSystem(true);
            Set<PermissionEnum> permissions = new HashSet<>(Arrays.asList(
                    PermissionEnum.VIEW_PRODUCT, PermissionEnum.VIEW_CATEGORY,
                    PermissionEnum.CREATE_ORDER, PermissionEnum.VIEW_ORDER, PermissionEnum.CANCEL_ORDER,
                    PermissionEnum.CREATE_RETURN, PermissionEnum.VIEW_RETURN,
                    PermissionEnum.VIEW_CUSTOMER, PermissionEnum.VIEW_SHIFT
            ));
            role.setPermissions(permissions);
            return roleRepository.save(role);
        });

        // 3. Create WH role
        Role whRole = roleRepository.findByName("ROLE_WH").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_WH");
            role.setDescription("Nhân viên Kho");
            role.setSystem(true);
            Set<PermissionEnum> permissions = new HashSet<>(Arrays.asList(
                    PermissionEnum.VIEW_PRODUCT, PermissionEnum.MANAGE_PRODUCT,
                    PermissionEnum.VIEW_CATEGORY, PermissionEnum.MANAGE_CATEGORY,
                    PermissionEnum.VIEW_RECEIPT, PermissionEnum.MANAGE_RECEIPT,
                    PermissionEnum.VIEW_SUPPLIER, PermissionEnum.MANAGE_SUPPLIER
            ));
            role.setPermissions(permissions);
            return roleRepository.save(role);
        });

        // 4. Create CS role
        Role csRole = roleRepository.findByName("ROLE_CS").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_CS");
            role.setDescription("Nhân viên CSKH");
            role.setSystem(true);
            Set<PermissionEnum> permissions = new HashSet<>(Arrays.asList(
                    PermissionEnum.VIEW_CUSTOMER, PermissionEnum.MANAGE_CUSTOMER,
                    PermissionEnum.VIEW_CAMPAIGN, PermissionEnum.MANAGE_CAMPAIGN,
                    PermissionEnum.VIEW_ORDER, PermissionEnum.VIEW_RETURN
            ));
            role.setPermissions(permissions);
            return roleRepository.save(role);
        });

        // 5. Migrate users: dùng native query đọc cột `role` cũ (legacy varchar) để migrate sang role_id mới
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getRole() == null) {
                // Đọc legacy role từ cột `role` (varchar) qua native query
                String legacyRoleName = jdbcTemplate.queryForObject(
                        "SELECT role FROM user WHERE id = ?", String.class, user.getId()
                );
                if (legacyRoleName != null) {
                    log.info("Migrating user {} from legacy role {}", user.getUsername(), legacyRoleName);
                    if ("ROLE_ADMIN".equals(legacyRoleName)) user.setRole(adminRole);
                    else if ("ROLE_SALE".equals(legacyRoleName)) user.setRole(saleRole);
                    else if ("ROLE_WH".equals(legacyRoleName)) user.setRole(whRole);
                    else if ("ROLE_CS".equals(legacyRoleName)) user.setRole(csRole);
                    userRepository.save(user);
                }
            }
        }

        log.info(">>> DataSeeder completed.");
    }
}
