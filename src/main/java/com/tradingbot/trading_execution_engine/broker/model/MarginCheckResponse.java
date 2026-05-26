package com.tradingbot.trading_execution_engine.broker.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarginCheckResponse {

    private Double totalMargin;

    private Double availableBalance;

    private Double insufficientBalance;

    private String message;

    public boolean hasInsufficientFunds() {
        return insufficientBalance != null && insufficientBalance > 0;
    }
}
