package com.tradingbot.trading_execution_engine.order.lifecycle;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!dhan")
public class NoOpOrderReconciliationService implements OrderReconciliationService {

    @Override
    public OrderReconciliationResult reconcileManualSquareOffs() {
        return OrderReconciliationResult.builder()
                .checkedOrders(0)
                .skippedOrders(0)
                .closedPositionsDetected(0)
                .cancelledExitLegs(0)
                .build();
    }
}
