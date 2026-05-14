package com.tradingbot.trading_execution_engine.decision;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PricePathAnalysis {

    private boolean entryTouched;

    private boolean stopLossBroken;

    private Double currentPrice;

    private Double maxBouncePercent;
}