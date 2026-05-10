package com.tradingbot.trading_execution_engine.repository;

import com.tradingbot.trading_execution_engine.entity.Signal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignalRepository extends JpaRepository<Signal, Long> {
}
