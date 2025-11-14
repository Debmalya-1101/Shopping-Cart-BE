package com.demoproject.shoppingcart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demoproject.shoppingcart.model.User;

public interface RegistrationRepository extends JpaRepository<User, Integer> {

	public User findByEmailId(String emailId);
	public User findByEmailIdAndPassword(String emailId, String password);

}
