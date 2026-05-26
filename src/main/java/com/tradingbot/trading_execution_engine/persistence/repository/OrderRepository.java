package com.tradingbot.trading_execution_engine.persistence.repository;

import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    List<Order> findByOrderStatus(String orderStatus);

    List<Order> findByOrderTypeAndOrderStatusIn(
            String orderType,
            Collection<String> orderStatuses
    );

    Optional<Order> findByBrokerOrderId(String brokerOrderId);
}
