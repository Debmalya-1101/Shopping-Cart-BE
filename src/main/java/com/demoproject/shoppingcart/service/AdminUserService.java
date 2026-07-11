package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.AdminUserDTO;
import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.model.Role;

public interface AdminUserService {
    PageResponse<AdminUserDTO> getUsers(Role role, Boolean active, String search, int page, int size);
    AdminUserDTO getUserById(Long id);
    AdminUserDTO updateUserRole(Long id, Role role);
    AdminUserDTO updateUserStatus(Long id, Boolean active);
}
