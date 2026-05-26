package com.tradingbot.trading_execution_engine.persistence.repository;

import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.OrderLeg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderLegRepository
        extends JpaRepository<OrderLeg, Long> {

    List<OrderLeg> findByOrder(Order order);

    Optional<OrderLeg> findByOrderAndLegName(
            Order order,
            String legName
    );

    List<OrderLeg> findByOrderAndLegNameInAndLegStatusIn(
            Order order,
            Collection<String> legNames,
            Collection<String> legStatuses
    );
}
