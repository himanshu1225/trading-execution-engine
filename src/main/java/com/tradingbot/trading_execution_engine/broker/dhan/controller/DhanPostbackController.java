package com.tradingbot.trading_execution_engine.broker.dhan.controller;

import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanOrderPostback;
import com.tradingbot.trading_execution_engine.broker.dhan.service.DhanPostbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dhan/postback")
@RequiredArgsConstructor
public class DhanPostbackController {

    private final DhanPostbackService dhanPostbackService;

    @PostMapping("/order-update")
    public ResponseEntity<String> orderUpdate(
            @RequestBody DhanOrderPostback postback) {

        dhanPostbackService.process(postback);

        return ResponseEntity.ok("Dhan postback processed");
    }
}
