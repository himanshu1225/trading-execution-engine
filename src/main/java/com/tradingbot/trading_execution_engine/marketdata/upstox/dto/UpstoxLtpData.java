package com.tradingbot.trading_execution_engine.marketdata.upstox.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpstoxLtpData {

    @JsonProperty("last_price")
    private Double lastPrice;

    @JsonProperty("instrument_token")
    private String instrumentToken;

    private Integer ltq;

    private Long volume;

    private Double cp;
}