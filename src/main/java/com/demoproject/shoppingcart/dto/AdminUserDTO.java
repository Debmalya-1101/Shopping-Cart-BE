package com.demoproject.shoppingcart.dto;

import com.demoproject.shoppingcart.model.Role;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {
    private Long id;
    private String emailId;
    private String userName;
    private Role role;
    private Boolean active;
    private String authProvider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
