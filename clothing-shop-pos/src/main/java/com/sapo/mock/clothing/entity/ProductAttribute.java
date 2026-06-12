package com.sapo.mock.clothing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_attribute",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "attr_key"}))
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "attr_key", nullable = false, length = 100)
    private String attrKey;

    @Column(name = "attr_value", nullable = false, columnDefinition = "TEXT")
    private String attrValue;
}
