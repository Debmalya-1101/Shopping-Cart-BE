package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.*;
import com.demoproject.shoppingcart.model.Role;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        // If no exception -> authentication successful
        User principal = (User) authentication.getPrincipal();

        // Our role is like "ROLE_USER"
        String role = principal.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_USER");

        String token = jwtUtil.generateToken(principal.getUsername(), role);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {

        if (userRepository.findByEmailId(request.getEmailId()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        if (userRepository.findByUserName(request.getUserName()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already in use");
        }

        AppUser user = new AppUser();
        user.setEmailId(request.getEmailId());
        user.setUserName(request.getUserName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER); // default

        userRepository.save(user);

        return ResponseEntity.ok("AppUser registered successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserInfoDTO> getLoggedInUserInfo() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {

            return ResponseEntity.status(401).build(); // unauthorized
        }

        String username = authentication.getName();

        // Load full AppUser from DB
        AppUser user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found in DB"));

        AuthUserInfoDTO response = new AuthUserInfoDTO(
                user.getUserName(),
                user.getEmailId(),
                user.getRole().name()
        );

        return ResponseEntity.ok(response);
    }

}
