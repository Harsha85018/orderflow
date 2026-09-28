package io.github.harsha85018.orderflow.inventory;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    protected Product() {}

    /** Takes stock if enough is available. Caller must hold the row lock. */
    public boolean tryReserve(int quantity) {
        if (availableQuantity < quantity) {
            return false;
        }
        availableQuantity -= quantity;
        return true;
    }

    public void release(int quantity) {
        availableQuantity += quantity;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAvailableQuantity() { return availableQuantity; }
}
