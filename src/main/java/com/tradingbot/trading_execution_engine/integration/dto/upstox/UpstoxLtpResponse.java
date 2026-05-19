package com.tradingbot.trading_execution_engine.integration.dto.upstox;

import lombok.Data;

import java.util.Map;

@Data
public class UpstoxLtpResponse {

    private String status;

    private Map<String, UpstoxLtpData> data;
}
