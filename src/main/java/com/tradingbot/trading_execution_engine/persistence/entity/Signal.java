package com.tradingbot.trading_execution_engine.persistence.entity;

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

    private String symbolDesc;

    private Double alertPrice;

    private Double entryPrice;

    private Double stopLossPrice;

    private Double locZoneHigh;

    private Double locZoneLow;

    private String tradeType;

    private Integer tradeScore;

    private String sector;

    private String alertDateTimeStamp;

    private Double actualEntryPrice;

    private Integer quantity;

    private Double riskPerShare;

    private Double oneRPrice;

    private Double onePointFiveRPrice;

    private Double twoRPrice;

    private Double targetPrice;

    private Double trailingJump;

    private String decisionReason;

    private String status;

    private LocalDateTime createdAt;
}
