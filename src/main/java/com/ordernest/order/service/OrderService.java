package com.ordernest.order.service;

import com.ordernest.order.client.InventoryClient;
import com.ordernest.order.client.InventoryProductResponse;
import com.ordernest.order.client.PaymentClient;
import com.ordernest.order.client.PaymentProcessRequest;
import com.ordernest.order.dto.CreateOrderRequest;
import com.ordernest.order.dto.CreateOrderResponse;
import com.ordernest.order.dto.OrderItemResponse;
import com.ordernest.order.dto.OrderResponse;
import com.ordernest.order.dto.PayOrderResponse;
import com.ordernest.order.entity.CustomerOrder;
import com.ordernest.order.entity.OrderStatus;
import com.ordernest.order.entity.PaymentStatus;
import com.ordernest.order.exception.BadRequestException;
import com.ordernest.order.exception.ResourceNotFoundException;
import com.ordernest.order.repository.OrderRepository;
import com.ordernest.order.security.JwtService;
import java.math.BigDecimal;
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
    private final PaymentClient paymentClient;
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
        order.setUnitPrice(resolvePrice(inventoryProduct));
        order.setCurrency(resolveCurrency(inventoryProduct));

        CustomerOrder saved = orderRepository.save(order);
        return new CreateOrderResponse(saved.getId());
    }

    @Transactional
    public PayOrderResponse payOrder(UUID orderId, String authorization) {
        UUID userId = extractUserIdFromAuthorization(authorization);
        CustomerOrder order = findById(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("You are not allowed to pay this order");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid");
        }

        ensurePricingAvailable(order, authorization);

        paymentClient.processPayment(new PaymentProcessRequest(order.getId()), authorization);

        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStatus(OrderStatus.PENDING);

        CustomerOrder saved = orderRepository.save(order);
        return new PayOrderResponse(
                saved.getId(),
                saved.getStatus(),
                saved.getPaymentStatus(),
                null,
                "Payment initiated"
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, String authorization) {
        CustomerOrder order = findById(orderId);
        ensurePricingAvailable(order, authorization);
        return mapToResponse(order);
    }

    @Transactional
    public List<OrderResponse> getOrdersByUserId(UUID userId, String authorization) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .peek(order -> ensurePricingAvailable(order, authorization))
                .map(this::mapToResponse)
                .toList();
    }

    private CustomerOrder findById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private OrderResponse mapToResponse(CustomerOrder order) {
        String currency = (order.getCurrency() == null || order.getCurrency().isBlank()) ? "INR" : order.getCurrency();
        BigDecimal totalAmount = null;
        if (order.getUnitPrice() != null && order.getQuantity() != null) {
            totalAmount = order.getUnitPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        }

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                new OrderItemResponse(order.getProductId(), order.getProductName(), order.getQuantity()),
                totalAmount,
                currency,
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

    private BigDecimal resolvePrice(InventoryProductResponse inventoryProduct) {
        if (inventoryProduct.price() == null || inventoryProduct.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Invalid product price from inventory");
        }
        return inventoryProduct.price();
    }

    private String resolveCurrency(InventoryProductResponse inventoryProduct) {
        if (inventoryProduct.currency() == null || inventoryProduct.currency().isBlank()) {
            return "INR";
        }
        return inventoryProduct.currency().trim().toUpperCase();
    }

    private void ensurePricingAvailable(CustomerOrder order, String authorization) {
        if (order.getUnitPrice() != null && order.getCurrency() != null && !order.getCurrency().isBlank()) {
            return;
        }

        InventoryProductResponse inventoryProduct = inventoryClient.getProductById(order.getProductId(), authorization);
        order.setUnitPrice(resolvePrice(inventoryProduct));
        order.setCurrency(resolveCurrency(inventoryProduct));
        orderRepository.save(order);
    }
}
