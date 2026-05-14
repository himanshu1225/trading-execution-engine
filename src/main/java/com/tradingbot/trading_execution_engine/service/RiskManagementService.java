package com.tradingbot.trading_execution_engine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RiskManagementService {

    @Value("${trading.capital}")
    private Double capital;

    @Value("${trading.risk-percent}")
    private Double riskPercent;

    public Integer calculateQuantity(
            Double entry,
            Double stopLoss) {

        Double riskAmount =
                capital * (riskPercent / 100);

        Double riskPerShare =
                entry - stopLoss;

        if (riskPerShare <= 0) {
            return 0;
        }

        return (int) (riskAmount / riskPerShare);
    }
}