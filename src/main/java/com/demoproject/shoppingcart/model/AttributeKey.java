package com.demoproject.shoppingcart.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attribute_keys")
@org.hibernate.annotations.BatchSize(size = 50)
public class AttributeKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String keyName; // Example: RAM, Battery, DriverSize

    @Enumerated(EnumType.STRING)
    private AttributeType type; // TEXT or NUMBER

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @JsonIgnore
    @OneToMany(mappedBy = "attributeKey", fetch = FetchType.LAZY)
    private List<ProductAttribute> productAttributes;
}
