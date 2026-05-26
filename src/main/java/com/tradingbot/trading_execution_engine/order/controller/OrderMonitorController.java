package com.tradingbot.trading_execution_engine.order.controller;

import com.tradingbot.trading_execution_engine.order.lifecycle.OrderMonitorService;
import com.tradingbot.trading_execution_engine.order.lifecycle.OrderReconciliationResult;
import com.tradingbot.trading_execution_engine.order.lifecycle.OrderReconciliationService;
import com.tradingbot.trading_execution_engine.order.lifecycle.SuperOrderLegLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderMonitorController {

    private final OrderMonitorService orderMonitorService;
    private final SuperOrderLegLifecycleService superOrderLegLifecycleService;
    private final OrderReconciliationService orderReconciliationService;

    @PostMapping("/orders/monitor")
    public String monitorOrders() {

        orderMonitorService.monitorPendingOrders();

        return "Order monitoring completed";
    }

    @PostMapping("/orders/{orderId}/super/exit-legs/cancel")
    public String cancelSuperOrderExitLegs(
            @PathVariable Long orderId) {

        int cancelledLegs =
                superOrderLegLifecycleService.cancelPendingExitLegs(orderId);

        return "Cancelled pending exit legs: " + cancelledLegs;
    }

    @PostMapping("/orders/reconcile")
    public OrderReconciliationResult reconcileOrders() {
        return orderReconciliationService.reconcileManualSquareOffs();
    }
}
