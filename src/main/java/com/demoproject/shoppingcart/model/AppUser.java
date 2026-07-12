package com.demoproject.shoppingcart.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
@org.hibernate.annotations.BatchSize(size = 100)
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String emailId; // Used as login username

	@Column(nullable = false, unique = true)
	private String userName;

	/** Bcrypt hash for LOCAL users. OAuth2 users have a random unguessable hash stored here. */
	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role = Role.ROLE_USER; // USER or ADMIN

	@Column(nullable = false)
	private Boolean active = true;

	// ============================================
	// OAuth2 / Social Login Fields
	// ============================================

	/** Which provider was used to authenticate this user. Defaults to LOCAL. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuthProvider authProvider = AuthProvider.LOCAL;

	/**
	 * The unique user identifier from the OAuth2 provider (e.g. Google's {@code sub} claim,
	 * Facebook's numeric {@code id}). Null for LOCAL users.
	 */
	@Column(length = 255)
	private String providerId;

	/** Profile picture URL provided by the OAuth2 provider. May change on subsequent logins. */
	@Column(length = 1024)
	private String avatarUrl;

	/** Full display name from the OAuth2 provider (e.g. "John Doe"). */
	@Column(length = 255)
	private String displayName;

	// ============================================
	// Relationships (Ownership + Security Control)
	// ============================================

	@JsonIgnore
	@OneToOne(mappedBy = "user", fetch = FetchType.LAZY,
			cascade = CascadeType.ALL, orphanRemoval = true)
	private Cart cart;

	@JsonIgnore
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY,
			cascade = CascadeType.ALL)
	private List<Order> orders;

	@JsonIgnore
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY,
			cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WishlistItem> wishlist;

	@JsonIgnore
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY,
			cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Address> addresses;

	// ============================================
	// Audit Information (Who + When)
	// ============================================

	@CreatedDate
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime updatedAt;

	@CreatedBy
	private String createdBy;

	@LastModifiedBy
	private String updatedBy;
}
