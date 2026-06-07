package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.CheckoutRequestDTO;
import com.demoproject.shoppingcart.dto.OrderDetailDTO;
import com.demoproject.shoppingcart.dto.OrderDetailItemDTO;
import com.demoproject.shoppingcart.dto.OrderItemDTO;
import com.demoproject.shoppingcart.dto.OrderResponseDTO;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.CartItemRepository;
import com.demoproject.shoppingcart.repository.CartRepository;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.OrderService;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;

    public OrderServiceImpl(UserRepository userRepository, CartRepository cartRepository, OrderRepository orderRepository, CartItemRepository cartItemRepository) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    @Transactional
    public OrderResponseDTO checkout(CheckoutRequestDTO request) {

        AppUser user = getLoggedInUser();

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (product.getStock() == null || product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for product: " + product.getName()
                );
            }
        }


        Order order = new Order();
        order.setUser(user);
        order.setName(request.getName());
        order.setPhoneNo(request.getPhoneNo());
        order.setEmail(request.getEmail());
        order.setAddress(request.getAddress());
        order.setStatus(OrderStatus.PLACED);

        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(cartItem.getProduct());
            oi.setQuantity(cartItem.getQuantity());
            oi.setPrice(cartItem.getPrice()); // snapshot
            return oi;
        }).toList();

        order.setItems(orderItems);

        Long total = orderItems.stream()
                .mapToLong(i -> i.getPrice() * i.getQuantity())
                .sum();

        order.setTotal(total);

        Order savedOrder = orderRepository.save(order);

        // Clear cart
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setTotalPrice(0L);
        cartRepository.save(cart);

        // 🔮 Kafka later:
        // orderEventProducer.sendOrderPlacedEvent(savedOrder);

        return convertToOrderDTO(savedOrder);
    }

    @Override
    public List<OrderResponseDTO> getMyOrders() {
        AppUser user = getLoggedInUser();

        return orderRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToOrderDTO)
                .toList();
    }

    @Override
    public OrderDetailDTO getOrderById(Long orderId) {
        AppUser user = getLoggedInUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        return convertToOrderDetailDTO(order);
    }


    private AppUser getLoggedInUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Lightweight DTO used for list view and checkout response ─────────────

    private OrderResponseDTO convertToOrderDTO(Order order) {

        List<OrderItemDTO> items = order.getItems().stream()
                .map(i -> new OrderItemDTO(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getImageUrl(),
                        i.getPrice(),
                        i.getQuantity(),
                        i.getPrice() * i.getQuantity()
                )).toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getTotal(),
                order.getStatus().name(),
                order.getCreatedAt(),
                items
        );
    }

    // ── Rich DTO used for the Order Details page ──────────────────────────────

    private OrderDetailDTO convertToOrderDetailDTO(Order order) {

        List<OrderDetailItemDTO> items = order.getItems().stream()
                .map(i -> {
                    Product product = i.getProduct();

                    // Resolve category name if the product has a category assigned
                    String categoryName = (product.getCategory() != null)
                            ? product.getCategory().getName()
                            : null;

                    return new OrderDetailItemDTO(
                            product.getId(),
                            product.getName(),
                            product.getImageUrl(),
                            categoryName,
                            i.getQuantity(),
                            i.getPrice(),
                            i.getPrice() * i.getQuantity()
                    );
                }).toList();

        return new OrderDetailDTO(
                order.getId(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getName(),
                order.getEmail(),
                order.getPhoneNo(),
                order.getAddress(),
                items,
                items.size(),
                order.getTotal()
        );
    }

}
