package com.sapo.mock.clothing.service;

import com.sapo.mock.clothing.domain.entity.Role;
import com.sapo.mock.clothing.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service xử lý các nghiệp vụ liên quan đến Role (vai trò phân quyền).
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Kiểm tra tên role đã tồn tại chưa.
     *
     * @param roleName tên role cần kiểm tra
     * @return true nếu đã tồn tại
     */
    public boolean isRoleNameExists(String roleName) {
        return roleRepository.existsByName(roleName);
    }

    /**
     * Tìm role theo tên.
     *
     * @param roleName tên role
     * @return Role nếu tìm thấy, null nếu không
     */
    public Role getRoleByName(String roleName) {
        return roleRepository.findByName(roleName);
    }

    /**
     * Tìm role theo ID.
     *
     * @param roleId ID của role
     * @return Role nếu tìm thấy, null nếu không
     */
    public Role getRoleById(Long roleId) {
        return roleRepository.findById(roleId).orElse(null);
    }

    /**
     * Tạo mới một role.
     *
     * @param newRole đối tượng Role cần tạo
     * @return Role đã lưu vào DB
     */
    public Role createRole(Role newRole) {
        return roleRepository.save(newRole);
    }

    /**
     * Cập nhật thông tin role.
     *
     * @param updatedRole đối tượng Role với thông tin mới
     * @return Role sau khi cập nhật, null nếu không tìm thấy
     */
    public Role updateRole(Role updatedRole) {
        Optional<Role> existingRole = roleRepository.findById(updatedRole.getId());
        if (existingRole.isEmpty()) {
            return null;
        }
        Role roleToUpdate = existingRole.get();
        roleToUpdate.setName(updatedRole.getName());
        roleToUpdate.setDescription(updatedRole.getDescription());
        roleToUpdate.setActive(updatedRole.isActive());
        roleToUpdate.setPermissions(updatedRole.getPermissions());
        return roleRepository.save(roleToUpdate);
    }

    /**
     * Xoá role theo ID.
     *
     * @param roleId ID của role cần xoá
     */
    public void deleteRole(Long roleId) {
        roleRepository.deleteById(roleId);
    }
}
