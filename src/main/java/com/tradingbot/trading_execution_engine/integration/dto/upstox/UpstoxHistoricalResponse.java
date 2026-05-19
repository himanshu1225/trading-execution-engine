package com.tradingbot.trading_execution_engine.integration.dto.upstox;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpstoxHistoricalResponse {

    private String status;

    private HistoricalData data;

    @Getter
    @Setter
    public static class HistoricalData {

        private List<List<Object>> candles;
    }
}