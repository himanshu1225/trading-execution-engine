package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DhanMarginResponse {

    private Double totalMargin;

    private Double spanMargin;

    private Double exposureMargin;

    private Double availableBalance;

    private Double variableMargin;

    private Double insufficientBalance;

    private Double brokerage;

    private String leverage;
}
