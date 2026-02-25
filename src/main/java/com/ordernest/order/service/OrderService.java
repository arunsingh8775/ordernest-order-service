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

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, String authorization) {
        InventoryProductResponse inventoryProduct = inventoryClient.getProductById(request.item().productId(), authorization);
        int available = inventoryProduct.availableQuantity() == null ? 0 : inventoryProduct.availableQuantity();
        int requested = request.item().quantity();

        if (requested > available) {
            throw new BadRequestException("Insufficient inventory. Available: " + available + ", requested: " + requested);
        }

        CustomerOrder order = new CustomerOrder();
        order.setUserId(request.userId());
        order.setProductId(request.item().productId());
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
}
