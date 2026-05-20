package com.tradingbot.trading_execution_engine.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brokerOrderId;

    private String symbol;

    private Double orderPrice;

    private Integer quantity;

    private String orderType;

    private String orderStatus;

    private LocalDateTime createdAt;

    private LocalDateTime placedAt;

    private LocalDateTime filledAt;

    private LocalDateTime cancelledAt;

    @ManyToOne
    @JoinColumn(name = "signal_id")
    private Signal signal;
}