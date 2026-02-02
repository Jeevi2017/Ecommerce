package com.example.Ecomm.entitiy;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // 🔥 REQUIRED FOR SIZE-BASED CART
    @Column(nullable = false, length = 10)
    private String size;

    @Transient
    public BigDecimal getItemTotal() {
        if (price != null && quantity != null) {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    public CartItem() {
        super();
    }

    public CartItem(Long id, Cart cart, Product product, Long quantity, BigDecimal price, String size) {
        this.id = id;
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
        this.size = size;
    }

    // ================= GETTERS & SETTERS =================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    // 🔥 SIZE GETTER / SETTER
    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    // ================= EQUALITY (SIZE AWARE) =================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CartItem that = (CartItem) o;

        return Objects.equals(
                cart != null ? cart.getId() : null,
                that.cart != null ? that.cart.getId() : null
        ) &&
                Objects.equals(
                        product != null ? product.getId() : null,
                        that.product != null ? that.product.getId() : null
                ) &&
                Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                cart != null ? cart.getId() : null,
                product != null ? product.getId() : null,
                size
        );
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "id=" + id +
                ", cartId=" + (cart != null ? cart.getId() : "null") +
                ", productId=" + (product != null ? product.getId() : "null") +
                ", size='" + size + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", itemTotal=" + getItemTotal() +
                '}';
    }
}
