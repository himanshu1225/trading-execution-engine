package com.tradingbot.trading_execution_engine.alert.service;

import com.tradingbot.trading_execution_engine.decision.model.TradeDecision;
import com.tradingbot.trading_execution_engine.decision.service.TradeDecisionEngine;
import com.tradingbot.trading_execution_engine.alert.dto.TradingViewAlert;
import com.tradingbot.trading_execution_engine.execution.service.ExecutionService;
import com.tradingbot.trading_execution_engine.persistence.entity.Signal;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import com.tradingbot.trading_execution_engine.order.model.SignalStatus;
import com.tradingbot.trading_execution_engine.persistence.repository.SignalRepository;
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

        boolean duplicate =
                signalRepository
                        .existsBySymbolAndEntryPriceAndStopLossPriceAndTradeTypeAndAlertDateTimeStamp(
                                alert.getSymbol(),
                                alert.getEntryPrice(),
                                alert.getStopLossPrice(),
                                alert.getTradeType(),
                                alert.getAlertDateTimeStamp()
                        );

        if (duplicate) {
            log.warn("Duplicate alert ignored for symbol={}",
                    alert.getSymbol());
            return;
        }

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

        signal.setRiskPerShare(
                decision.getRiskPerShare()
        );

        signal.setOneRPrice(
                decision.getOneRPrice()
        );

        signal.setOnePointFiveRPrice(
                decision.getOnePointFiveRPrice()
        );

        signal.setTwoRPrice(
                decision.getTwoRPrice()
        );

        signal.setTargetPrice(
                decision.getTargetPrice()
        );

        signal.setTrailingJump(
                decision.getTrailingJump()
        );

        signal.setDecisionReason(
                decision.getDecisionReason()
        );

        if (!decision.isValid()) {
            signal.setStatus(SignalStatus.REJECTED.name());

        } else if (OrderType.MARKET.name().equals(decision.getActionType())) {
            signal.setStatus(SignalStatus.MARKET_EXECUTED.name());

        } else if (OrderType.LIMIT.name().equals(decision.getActionType())) {
            signal.setStatus(SignalStatus.PENDING_LIMIT.name());
        }

        signal.setCreatedAt(LocalDateTime.now());

        return signal;
    }
}
