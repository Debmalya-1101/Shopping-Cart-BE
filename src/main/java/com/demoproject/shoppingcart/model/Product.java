package com.demoproject.shoppingcart.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
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
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	private String name;

	/**
	 * Full Amazon product title, e.g.:
	 * "Samsung Galaxy M07 Mobile (Black, 4GB RAM, 64GB Storage) | MediaTek Helio G99 | ..."
	 * Stored separately so the short display name stays clean.
	 */
	@Column(name = "full_name", columnDefinition = "TEXT")
	private String fullName;

	@NotNull
	@Positive
	private Long price;

	private String imageUrl;

	@Column(columnDefinition = "TEXT")
	private String description;

	private String brand;

	private Integer stock;

	private Boolean active = true;

	private Double rating;

	@Column(name = "rating_count")
	private Long ratingCount = 0L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@JsonIgnore
	@OneToMany(mappedBy = "product", fetch = FetchType.LAZY,
			cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductAttribute> attributes = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "product", fetch = FetchType.LAZY,
			cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductImage> images = new ArrayList<>();

	// Helper methods to avoid broken relationships
	public void addAttribute(ProductAttribute attribute) {
		attributes.add(attribute);
		attribute.setProduct(this);
	}

	public void addImage(ProductImage image) {
		images.add(image);
		image.setProduct(this);
	}

	@CreatedDate
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime updatedAt;

	@CreatedBy
	private String createdBy;

	@LastModifiedBy
	private String updatedBy;
}
