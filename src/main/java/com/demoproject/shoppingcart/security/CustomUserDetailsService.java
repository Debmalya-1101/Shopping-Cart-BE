package com.demoproject.shoppingcart.security;

import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        AppUser user = userRepository
                .findByEmailIdOrUserName(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("AppUser not found: " + usernameOrEmail));

        // ROLE_USER / ROLE_ADMIN
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().name());

        return new User(
                user.getUserName(),   // principal username
                user.getPassword(),   // bcrypt hash
                List.of(authority)
        );
    }
}
