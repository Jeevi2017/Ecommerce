package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.OrderDTO;
import com.example.Ecomm.entitiy.*;
import com.example.Ecomm.entitiy.Order.OrderStatus;
import com.example.Ecomm.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock private OrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CartRepository cartRepository;
    @Mock private AddressRepository addressRepository;

    // ---------- HELPERS ----------

    private Customer mockCustomer() {
        Customer c = new Customer();
        c.setId(1L);
        return c;
    }

    private Address mockAddress(Customer customer) {
        Address a = new Address();
        Profile profile = new Profile();
        profile.setCustomer(customer);
        a.setProfile(profile);
        a.setStreet("Street");
        a.setCity("City");
        a.setState("State");
        a.setPostalCode("123456");
        return a;
    }

    private Product mockProduct() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Product");
        p.setPrice(BigDecimal.valueOf(100));
        p.setStockQuantity(10L);
        return p;
    }

    private Cart mockCart(Product product) {
        Cart cart = new Cart();
        cart.setTotalAmount(BigDecimal.valueOf(100));
        cart.setDiscountAmount(BigDecimal.ZERO);

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1L);
        item.setPrice(BigDecimal.valueOf(100));

        cart.addCartItem(item);
        return cart;
    }

    private Order mockOrder(Customer customer) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(100));
        return order;
    }

    // ---------- TESTS ----------

    @Test
    void placeOrder() {
        Customer customer = mockCustomer();
        Address address = mockAddress(customer);
        Product product = mockProduct();
        Cart cart = mockCart(product);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        OrderDTO dto = orderService.placeOrder(1L, 1L);

        assertNotNull(dto);
        assertEquals("PENDING", dto.getStatus());
    }

    @Test
    void getOrderById() {
        Order order = mockOrder(mockCustomer());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderDTO dto = orderService.getOrderById(1L);
        assertNotNull(dto);
    }

    @Test
    void getOrdersByCustomerId() {
        Order order = mockOrder(mockCustomer());
        when(orderRepository.findByCustomer_Id(1L)).thenReturn(List.of(order));

        List<OrderDTO> result = orderService.getOrdersByCustomerId(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getAllOrders() {
        Order order = mockOrder(mockCustomer());
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderDTO> result = orderService.getAllOrders();
        assertEquals(1, result.size());
    }

    @Test
    void updateOrderStatus() {
        Order order = mockOrder(mockCustomer());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        OrderDTO dto = orderService.updateOrderStatus(1L, "DELIVERED"); // ✅ FIX
        assertNotNull(dto);
    }

    @Test
    void updateOrderFull() {
        Order order = mockOrder(mockCustomer());
        OrderDTO update = new OrderDTO();
        update.setStatus("DELIVERED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        OrderDTO dto = orderService.updateOrderFull(1L, update);
        assertNotNull(dto);
    }

    @Test
    void cancelOrder() {
        Order order = mockOrder(mockCustomer());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        assertDoesNotThrow(() -> orderService.cancelOrder(1L));
    }

    @Test
    void deleteOrder() {
        Order order = mockOrder(mockCustomer());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertDoesNotThrow(() -> orderService.deleteOrder(1L));
    }

    @Test
    void getCustomerIdForOrderInternal() {
        Customer customer = mockCustomer();
        Order order = mockOrder(customer);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Long customerId = orderService.getCustomerIdForOrderInternal(1L);
        assertEquals(1L, customerId);
    }
}
