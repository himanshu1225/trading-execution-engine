package com.tradingbot.trading_execution_engine.controller;

import com.tradingbot.trading_execution_engine.dto.TradingViewAlert;
import com.tradingbot.trading_execution_engine.service.SignalService;
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