package com.ordernest.order.service;

import com.ordernest.order.client.InventoryClient;
import com.ordernest.order.client.InventoryProductResponse;
import com.ordernest.order.dto.CreateOrderRequest;
import com.ordernest.order.dto.CreateOrderResponse;
import com.ordernest.order.dto.OrderItemResponse;
import com.ordernest.order.dto.OrderResponse;
import com.ordernest.order.entity.CustomerOrder;
import com.ordernest.order.exception.BadRequestException;
import com.ordernest.order.exception.ResourceNotFoundException;
import com.ordernest.order.repository.OrderRepository;
import com.ordernest.order.security.JwtService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final JwtService jwtService;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, String authorization) {
        UUID userId = extractUserIdFromAuthorization(authorization);
        InventoryProductResponse inventoryProduct = inventoryClient.getProductById(request.item().productId(), authorization);
        int available = inventoryProduct.availableQuantity() == null ? 0 : inventoryProduct.availableQuantity();
        int requested = request.item().quantity();

        if (requested > available) {
            throw new BadRequestException("Insufficient inventory. Available: " + available + ", requested: " + requested);
        }

        int updatedAvailableQuantity = available - requested;
        inventoryClient.updateProductStock(request.item().productId(), updatedAvailableQuantity, authorization);

        CustomerOrder order = new CustomerOrder();
        order.setUserId(userId);
        order.setProductId(request.item().productId());
        order.setProductName(inventoryProduct.name());
        order.setQuantity(requested);

        CustomerOrder saved = orderRepository.save(order);
        return new CreateOrderResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        return mapToResponse(findById(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private CustomerOrder findById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private OrderResponse mapToResponse(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                new OrderItemResponse(order.getProductId(), order.getQuantity()),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getCreatedAt()
        );
    }

    private UUID extractUserIdFromAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BadRequestException("Missing or invalid Authorization header");
        }

        String token = authorization.substring(7);
        try {
            UUID userId = jwtService.extractUserId(token);
            if (userId == null) {
                throw new BadRequestException("userId not found in token");
            }
            return userId;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid userId in token");
        } catch (Exception ex) {
            throw new BadRequestException("Unable to resolve userId from token");
        }
    }
}
