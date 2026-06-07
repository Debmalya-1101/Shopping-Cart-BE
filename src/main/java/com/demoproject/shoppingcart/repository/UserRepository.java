package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailId(String emailId);

    Optional<AppUser> findByUserName(String userName);

    // For login using either
    Optional<AppUser> findByEmailIdOrUserName(String emailId, String userName);

    // Count users excluding admins
    long countByRoleNot(Role role);
}


