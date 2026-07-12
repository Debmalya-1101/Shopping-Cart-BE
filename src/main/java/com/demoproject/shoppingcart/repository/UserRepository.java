package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.AuthProvider;
import com.demoproject.shoppingcart.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailId(String emailId);

    Optional<AppUser> findByUserName(String userName);

    // For login using either
    Optional<AppUser> findByEmailIdOrUserName(String emailId, String userName);

    // Count users excluding admins
    long countByRoleNot(Role role);

    // Count users by role and active status
    long countByRoleAndActive(Role role, Boolean active);

    /**
     * Looks up a user by their provider-issued ID and the provider name.
     * Used during OAuth2 login to find an existing linked account.
     */
    Optional<AppUser> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);

    @Query("SELECT u FROM AppUser u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:active IS NULL OR u.active = :active) AND " +
           "(:search IS NULL OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.emailId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AppUser> findByFilters(@Param("role") Role role, @Param("active") Boolean active, @Param("search") String search, Pageable pageable);
}


