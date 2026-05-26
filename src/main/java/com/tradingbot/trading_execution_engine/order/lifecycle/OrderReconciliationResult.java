package com.tradingbot.trading_execution_engine.order.lifecycle;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderReconciliationResult {

    private int checkedOrders;

    private int skippedOrders;

    private int closedPositionsDetected;

    private int cancelledExitLegs;
}
