package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.dto.TradingViewAlert;
import com.tradingbot.trading_execution_engine.entity.Signal;
import com.tradingbot.trading_execution_engine.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignalService {

    private final SignalRepository signalRepository;

    private final ValidationService validationService;

    public void processAlert(TradingViewAlert alert) {

        Signal signal = new Signal();

        signal.setSymbol(alert.getSymbol());
        signal.setEntryPrice(alert.getEntryPrice());
        signal.setStopLoss(alert.getStopLoss());
        signal.setZoneHigh(alert.getZoneHigh());
        signal.setZoneLow(alert.getZoneLow());
        signal.setSetupType(alert.getSetupType());


        signal.setAlertTime(LocalDateTime.now());
        boolean valid =
                validationService.validateEntry(alert);

        if (valid) {
            signal.setStatus("VALIDATED");
        } else {
            signal.setStatus("REJECTED");
        }
        signalRepository.save(signal);
    }
}