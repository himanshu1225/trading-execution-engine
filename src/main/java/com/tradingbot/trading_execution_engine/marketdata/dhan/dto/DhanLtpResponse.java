package com.tradingbot.trading_execution_engine.marketdata.dhan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DhanLtpResponse {

    private Map<String, Map<String, DhanLtpData>> data;

    private String status;

    @Getter
    @Setter
    public static class DhanLtpData {

        @JsonProperty("last_price")
        private Double lastPrice;
    }
}
