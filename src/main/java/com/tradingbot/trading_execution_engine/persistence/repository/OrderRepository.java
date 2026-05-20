package com.tradingbot.trading_execution_engine.persistence.repository;

import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    List<Order> findByOrderStatus(String orderStatus);
}