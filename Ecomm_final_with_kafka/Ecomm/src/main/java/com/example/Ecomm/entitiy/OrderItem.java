package com.example.Ecomm.entitiy;

import java.math.BigDecimal;
import java.util.Objects; // NEW: Import Objects for equals/hashCode

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient; // Keep if getItemTotal is still needed/used

@Entity
@Table(name ="order_item")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_item_id")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "quantity", nullable = false)
	private Long quantity; 

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@ManyToOne
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	public OrderItem() {
	}

	public OrderItem(Long id, Product product, Long quantity, BigDecimal price, Order order) { 
		this.id = id;
		this.product = product;
		this.quantity = quantity;
		this.price = price;
		this.order = order;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	@Transient
	public BigDecimal getItemTotal() {
		if (price != null && quantity != null) {
			return price.multiply(BigDecimal.valueOf(quantity));
		}
		return BigDecimal.ZERO;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OrderItem orderItem = (OrderItem) o;
		return Objects.equals(id, orderItem.id) &&
			   Objects.equals(order != null ? order.getId() : null, orderItem.order != null ? orderItem.order.getId() : null) &&
			   Objects.equals(product != null ? product.getId() : null, orderItem.product != null ? orderItem.product.getId() : null);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, order != null ? order.getId() : null, product != null ? product.getId() : null);
	}

	@Override
	public String toString() {
		return "OrderItem{" +
			   "id=" + id +
			   ", productId=" + (product != null ? product.getId() : "null") +
			   ", quantity=" + quantity +
			   ", price=" + price +
			   ", orderId=" + (order != null ? order.getId() : "null") +
			   '}';
	}
}
