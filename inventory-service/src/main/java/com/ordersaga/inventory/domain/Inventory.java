package com.ordersaga.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private Integer availableQuantity;

    protected Inventory() {
    }

    public Inventory(String sku, Integer availableQuantity) {
        this.sku = sku;
        this.availableQuantity = availableQuantity;
    }

    public void deduct(Integer quantity) {
        if (availableQuantity < quantity) {
            throw new IllegalStateException("not enough inventory for sku: " + sku);
        }

        this.availableQuantity = this.availableQuantity - quantity;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
}
