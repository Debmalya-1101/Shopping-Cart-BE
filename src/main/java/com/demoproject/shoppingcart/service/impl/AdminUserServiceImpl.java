package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AdminUserDTO;
import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.Role;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    public AdminUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public PageResponse<AdminUserDTO> getUsers(Role role, Boolean active, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<AppUser> userPage = userRepository.findByFilters(role, active, search, pageable);
        
        List<AdminUserDTO> dtoList = userPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
        return new PageResponse<>(
                dtoList,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    @Override
    public AdminUserDTO getUserById(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToDTO(user);
    }

    @Override
    public AdminUserDTO updateUserRole(Long id, Role role) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setRole(role);
        userRepository.save(user);
        return convertToDTO(user);
    }

    @Override
    public AdminUserDTO updateUserStatus(Long id, Boolean active) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setActive(active);
        userRepository.save(user);
        return convertToDTO(user);
    }

    private AdminUserDTO convertToDTO(AppUser user) {
        return new AdminUserDTO(
                user.getId(),
                user.getEmailId(),
                user.getUserName(),
                user.getRole(),
                user.getActive(),
                user.getAuthProvider().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
