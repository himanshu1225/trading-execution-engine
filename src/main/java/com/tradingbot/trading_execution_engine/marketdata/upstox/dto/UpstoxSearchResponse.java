package com.tradingbot.trading_execution_engine.marketdata.upstox.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpstoxSearchResponse {

    private String status;

    private List<InstrumentData> data;

    @Getter
    @Setter
    public static class InstrumentData {

        private String instrument_key;

        private String trading_symbol;

        private String exchange;
    }
}