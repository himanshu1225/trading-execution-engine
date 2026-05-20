package com.tradingbot.trading_execution_engine.marketdata.dhan.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DhanIntradayResponse {

    private List<Double> open;

    private List<Double> high;

    private List<Double> low;

    private List<Double> close;

    private List<Long> timestamp;
}
