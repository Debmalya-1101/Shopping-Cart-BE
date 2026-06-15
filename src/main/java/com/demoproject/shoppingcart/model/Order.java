package com.demoproject.shoppingcart.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private AppUser user;

	@org.hibernate.annotations.BatchSize(size = 100)
	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
			cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	private Long total;

	// Shipping snapshot info
	private String name;
	private Long phoneNo;
	private String email;
	private String address;

	@Enumerated(EnumType.STRING)
	private OrderStatus status = OrderStatus.PLACED;
	@Enumerated(EnumType.STRING)
	private PaymentStatus paymentStatus = PaymentStatus.INITIATED;

	// Payment improvements
	private String paymentReferenceId; // Unique reference ID from payment gateway
	private LocalDateTime paymentInitiatedAt; // When payment was initiated
	private LocalDateTime paymentCompletedAt; // When payment was completed
	private Integer retryCount = 0; // Number of retry attempts for payment

	public void addItem(OrderItem item) {
		items.add(item);
		item.setOrder(this);
	}

	@CreatedDate
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime updatedAt;

	@jakarta.persistence.Version
	private Long version;
}


