package com.demoproject.shoppingcart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
@EntityScan(basePackages = {"com.demoproject.shoppingcart.model", "com.demoproject.shoppingcart.notification.entity"})
@EnableJpaRepositories(basePackages = {"com.demoproject.shoppingcart.repository", "com.demoproject.shoppingcart.notification.repository"})
@EnableRetry
@EnableScheduling
@EnableAsync
public class ShoppingCartApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShoppingCartApplication.class, args);
	}

}
