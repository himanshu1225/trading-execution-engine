package com.tradingbot.trading_execution_engine.alert.controller;

import com.tradingbot.trading_execution_engine.alert.dto.TradingViewAlert;
import com.tradingbot.trading_execution_engine.alert.service.SignalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class TradingViewWebhookController {

    private final SignalService signalService;

    @PostMapping("/tradingview")
    public ResponseEntity<String> receiveAlert(
            @RequestBody TradingViewAlert alert) {

        signalService.processAlert(alert);

        return ResponseEntity.ok("Alert received successfully");
    }
}