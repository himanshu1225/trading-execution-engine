package com.tradingbot.trading_execution_engine.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_legs")
@Getter
@Setter
public class OrderLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brokerOrderId;

    private String legName;

    private String legStatus;

    private Double price;

    private Double trailingJump;

    private Integer quantity;

    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
