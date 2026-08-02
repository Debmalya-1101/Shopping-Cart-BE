package com.demoproject.shoppingcart.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_chat_context")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserChatContext {

    @Id
    private Long userId;

    private LocalDateTime lastAccessed;

    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String messages;

    @Version
    private Long version;
}
