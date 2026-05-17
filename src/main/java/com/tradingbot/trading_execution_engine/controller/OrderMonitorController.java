package com.tradingbot.trading_execution_engine.controller;

import com.tradingbot.trading_execution_engine.service.OrderMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderMonitorController {

    private final OrderMonitorService orderMonitorService;

    @PostMapping("/orders/monitor")
    public String monitorOrders() {

        orderMonitorService.monitorPendingOrders();

        return "Order monitoring completed";
    }
}