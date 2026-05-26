package com.tradingbot.trading_execution_engine.decision.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TradeTargetPlan {

    private Double riskPerShare;

    private Double oneRPrice;

    private Double onePointFiveRPrice;

    private Double twoRPrice;

    private Double targetPrice;

    private Double trailingJump;
}
