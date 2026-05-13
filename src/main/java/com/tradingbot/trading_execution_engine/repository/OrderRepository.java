package com.tradingbot.trading_execution_engine.repository;

import com.tradingbot.trading_execution_engine.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

}