package com.tradingbot.trading_execution_engine.order.lifecycle;

import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;
import com.tradingbot.trading_execution_engine.order.model.OrderLegStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderStatus;

import java.util.List;

public final class OrderLifecycleRules {

    public static final List<String> ACTIVE_SUPER_ORDER_STATUSES =
            List.of(
                    OrderStatus.PENDING.name(),
                    OrderStatus.PLACED.name(),
                    OrderStatus.PARTIAL_FILLED.name()
            );

    public static final List<String> EXIT_LEG_NAMES =
            List.of(
                    SuperOrderLeg.TARGET_LEG.name(),
                    SuperOrderLeg.STOP_LOSS_LEG.name()
            );

    public static final List<String> PENDING_LEG_STATUSES =
            List.of(OrderLegStatus.PENDING.name());

    private OrderLifecycleRules() {
    }
}
