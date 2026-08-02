package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.UserChatContext;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChatContextRepository extends JpaRepository<UserChatContext, Long> {
}
