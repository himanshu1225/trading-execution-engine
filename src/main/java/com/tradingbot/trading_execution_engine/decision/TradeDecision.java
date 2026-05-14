package com.tradingbot.trading_execution_engine.decision;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TradeDecision {

    private boolean valid;

    private String actionType;

    private Double actualEntryPrice;

    private Integer quantity;

    private String decisionReason;
}