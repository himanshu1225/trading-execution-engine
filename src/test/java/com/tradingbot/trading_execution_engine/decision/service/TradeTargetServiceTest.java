package com.tradingbot.trading_execution_engine.decision.service;

import com.tradingbot.trading_execution_engine.decision.model.TradeTargetPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradeTargetServiceTest {

    private final TradeTargetService tradeTargetService =
            new TradeTargetService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                tradeTargetService,
                "trailingJumpPercent",
                0.5
        );
    }

    @Test
    void calculatesUniformTwoRTargetAndTrailingPlan() {
        TradeTargetPlan plan =
                tradeTargetService.calculate(100.0, 90.0);

        assertThat(plan.getRiskPerShare()).isEqualTo(10.0);
        assertThat(plan.getOneRPrice()).isEqualTo(110.0);
        assertThat(plan.getOnePointFiveRPrice()).isEqualTo(115.0);
        assertThat(plan.getTwoRPrice()).isEqualTo(120.0);
        assertThat(plan.getTargetPrice()).isEqualTo(120.0);
        assertThat(plan.getTrailingJump()).isEqualTo(0.5);
    }

    @Test
    void rejectsInvalidBuyRisk() {
        assertThatThrownBy(() -> tradeTargetService.calculate(100.0, 100.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stop loss must be below entry");
    }
}
