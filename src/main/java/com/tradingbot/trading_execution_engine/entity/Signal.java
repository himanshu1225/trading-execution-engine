package com.tradingbot.trading_execution_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "signals")
@Getter
@Setter
public class Signal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    private Double entryPrice;

    private Double stopLoss;

    private Double zoneHigh;

    private Double zoneLow;

    private String setupType;

    private String status;

    private LocalDateTime alertTime;
}