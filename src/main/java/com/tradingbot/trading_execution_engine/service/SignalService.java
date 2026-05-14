package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.dto.TradingViewAlert;
import com.tradingbot.trading_execution_engine.entity.Signal;
import com.tradingbot.trading_execution_engine.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignalService {

    private final SignalRepository signalRepository;
    private final ValidationService validationService;
    private final ExecutionService executionService;

    public void processAlert(TradingViewAlert alert) {

        log.info("Received TradingView alert for symbol={}, tradeType={}, tradeScore={}",
                alert.getSymbol(),
                alert.getTradeType(),
                alert.getTradeScore());

        boolean valid = validationService.validateEntry(alert);

        Signal signal = buildSignal(alert, valid);

        Signal savedSignal = signalRepository.save(signal);

        log.info("Signal persisted with id={} and status={}",
                savedSignal.getId(),
                savedSignal.getStatus());

        if ("VALIDATED".equals(savedSignal.getStatus())) {
            try {
                executionService.execute(savedSignal);

                log.info("Execution triggered for signalId={}",
                        savedSignal.getId());

            } catch (Exception e) {
                log.error("Execution failed for signalId={}",
                        savedSignal.getId(),
                        e);
            }
        } else {
            log.info("Signal rejected for symbol={}",
                    savedSignal.getSymbol());
        }
    }

    private Signal buildSignal(TradingViewAlert alert, boolean valid) {

        Signal signal = new Signal();

        signal.setSymbol(alert.getSymbol());
        signal.setSymbolDesc(alert.getSymbolDesc());
        signal.setAlertPrice(alert.getAlertPrice());
        signal.setEntryPrice(alert.getEntryPrice());
        signal.setStopLossPrice(alert.getStopLossPrice());
        signal.setLocZoneHigh(alert.getLocZoneHigh());
        signal.setLocZoneLow(alert.getLocZoneLow());
        signal.setTradeType(alert.getTradeType());
        signal.setTradeScore(alert.getTradeScore());
        signal.setSector(alert.getSector());
        signal.setAlertDateTimeStamp(alert.getAlertDateTimeStamp());

        signal.setCreatedAt(LocalDateTime.now());

        signal.setStatus(valid ? "VALIDATED" : "REJECTED");

        return signal;
    }
}