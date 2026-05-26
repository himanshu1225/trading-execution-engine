package com.tradingbot.trading_execution_engine.decision.service;

import com.tradingbot.trading_execution_engine.alert.dto.TradingViewAlert;
import com.tradingbot.trading_execution_engine.decision.model.PricePathAnalysis;
import com.tradingbot.trading_execution_engine.decision.model.TradeDecision;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import com.tradingbot.trading_execution_engine.risk.service.RiskManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeDecisionEngineTest {

    @Mock
    private PricePathAnalyzer pricePathAnalyzer;

    @Mock
    private RiskManagementService riskManagementService;

    private TradeDecisionEngine tradeDecisionEngine;

    @BeforeEach
    void setUp() {
        TradeTargetService tradeTargetService =
                new TradeTargetService();
        ReflectionTestUtils.setField(
                tradeTargetService,
                "trailingJumpPercent",
                0.5
        );

        tradeDecisionEngine =
                new TradeDecisionEngine(
                        pricePathAnalyzer,
                        riskManagementService,
                        tradeTargetService
                );

        ReflectionTestUtils.setField(
                tradeDecisionEngine,
                "thresholdPercent",
                1.0
        );
    }

    @Test
    void rejectsTradeScoreBelowFiveBeforeMarketDataLookup() {
        TradingViewAlert alert = alert(4);

        TradeDecision decision =
                tradeDecisionEngine.evaluate(alert);

        assertThat(decision.isValid()).isFalse();
        assertThat(decision.getDecisionReason()).isEqualTo("TRADE_SCORE_BELOW_5");
        verify(pricePathAnalyzer, never()).analyze(alert);
    }

    @Test
    void acceptedBetterEntryIncludesTwoRTargetPlan() {
        TradingViewAlert alert = alert(8);

        when(pricePathAnalyzer.analyze(alert))
                .thenReturn(PricePathAnalysis.builder()
                        .entryTouched(true)
                        .stopLossBroken(false)
                        .currentPrice(100.0)
                        .maxBouncePercent(0.0)
                        .build());
        when(riskManagementService.calculateQuantity(100.0, 90.0))
                .thenReturn(10);

        TradeDecision decision =
                tradeDecisionEngine.evaluate(alert);

        assertThat(decision.isValid()).isTrue();
        assertThat(decision.getActionType()).isEqualTo(OrderType.MARKET.name());
        assertThat(decision.getActualEntryPrice()).isEqualTo(100.0);
        assertThat(decision.getRiskPerShare()).isEqualTo(10.0);
        assertThat(decision.getOneRPrice()).isEqualTo(110.0);
        assertThat(decision.getOnePointFiveRPrice()).isEqualTo(115.0);
        assertThat(decision.getTwoRPrice()).isEqualTo(120.0);
        assertThat(decision.getTargetPrice()).isEqualTo(120.0);
        assertThat(decision.getTrailingJump()).isEqualTo(0.5);
    }

    private TradingViewAlert alert(Integer tradeScore) {
        TradingViewAlert alert = new TradingViewAlert();
        alert.setSymbol("RELIANCE");
        alert.setEntryPrice(105.0);
        alert.setStopLossPrice(90.0);
        alert.setTradeScore(tradeScore);
        return alert;
    }
}
