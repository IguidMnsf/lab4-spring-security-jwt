package com.example.lab4.service;

import com.example.lab4.dto.OrderCreateRequest;
import com.example.lab4.dto.OrderItemResponse;
import com.example.lab4.dto.OrderResponse;
import com.example.lab4.entity.CustomerOrder;
import com.example.lab4.entity.OrderItem;
import com.example.lab4.entity.Product;
import com.example.lab4.entity.AppUser;
import com.example.lab4.exception.ResourceNotFoundException;
import com.example.lab4.repository.CustomerOrderRepository;
import com.example.lab4.repository.ProductRepository;
import com.example.lab4.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(
            CustomerOrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public OrderResponse create(OrderCreateRequest request, String username) {
        AppUser owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
        CustomerOrder order = new CustomerOrder(LocalDateTime.now(), owner);
        for (var itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produit introuvable avec l'id : " + itemRequest.productId()
                    ));
            order.addItem(new OrderItem(product, itemRequest.quantity(), product.getPrice()));
        }
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id, String username, boolean admin) {
        CustomerOrder order = (admin
                ? orderRepository.findById(id)
                : orderRepository.findByIdAndOwnerUsername(id, username))
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable avec l'id : " + id));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findForUser(String username, boolean admin) {
        List<CustomerOrder> orders = admin
                ? orderRepository.findAll()
                : orderRepository.findAllByOwnerUsernameOrderByOrderDateDesc(username);
        return orders.stream().map(this::toResponse).toList();
    }

    public void delete(Long id) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable avec l'id : " + id));
        orderRepository.delete(order);
    }

    private OrderResponse toResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();
        BigDecimal total = items.stream()
                .map(OrderItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(order.getId(), order.getOrderDate(), items, total);
    }
}
