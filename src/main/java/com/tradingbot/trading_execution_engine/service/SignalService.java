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
    private final ExecutionService executionService;

    public void processAlert(TradingViewAlert alert) {

        boolean valid = validationService.validateEntry(alert);

        Signal signal = new Signal();

        signal.setSymbol(alert.getSymbol());
        signal.setEntryPrice(alert.getEntryPrice());
        signal.setStopLoss(alert.getStopLoss());
        signal.setZoneHigh(alert.getZoneHigh());
        signal.setZoneLow(alert.getZoneLow());
        signal.setSetupType(alert.getSetupType());
        signal.setAlertTime(LocalDateTime.now());

        signal.setStatus(valid ? "VALIDATED" : "REJECTED");

        Signal savedSignal = signalRepository.save(signal);

        if ("VALIDATED".equals(savedSignal.getStatus())) {
            executionService.execute(savedSignal);
        } else{
            System.out.println("Order Rejected!!");
        }
    }
}