package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.decision.TradeDecision;
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
    private final TradeDecisionEngine tradeDecisionEngine;
    private final ExecutionService executionService;

    public void processAlert(TradingViewAlert alert) {

        log.info(
                "Received TradingView alert for symbol={}, tradeType={}, tradeScore={}",
                alert.getSymbol(),
                alert.getTradeType(),
                alert.getTradeScore()
        );

        TradeDecision decision =
                tradeDecisionEngine.evaluate(alert);

        Signal signal =
                buildSignal(alert, decision);

        Signal savedSignal =
                signalRepository.save(signal);

        log.info(
                "Signal persisted with id={}, status={}, reason={}",
                savedSignal.getId(),
                savedSignal.getStatus(),
                savedSignal.getDecisionReason()
        );

        if (decision.isValid()) {
            try {
                executionService.execute(savedSignal, decision);

                log.info(
                        "Execution triggered for signalId={}",
                        savedSignal.getId()
                );

            } catch (Exception e) {
                log.error(
                        "Execution failed for signalId={}",
                        savedSignal.getId(),
                        e
                );
            }
        }
    }

    private Signal buildSignal(
            TradingViewAlert alert,
            TradeDecision decision) {

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

        signal.setActualEntryPrice(
                decision.getActualEntryPrice()
        );

        signal.setQuantity(
                decision.getQuantity()
        );

        signal.setDecisionReason(
                decision.getDecisionReason()
        );

        signal.setStatus(
                decision.isValid()
                        ? "VALIDATED"
                        : "REJECTED"
        );

        signal.setCreatedAt(LocalDateTime.now());

        return signal;
    }
}