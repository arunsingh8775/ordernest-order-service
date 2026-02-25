package com.ordernest.order.repository;

import com.ordernest.order.entity.CustomerOrder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
