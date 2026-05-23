package com.tradingbot.trading_execution_engine.execution.service;

import com.tradingbot.trading_execution_engine.broker.model.BrokerProductType;
import com.tradingbot.trading_execution_engine.persistence.entity.Signal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionProductResolverTest {

    private final ExecutionProductResolver resolver =
            new ExecutionProductResolver();

    @Test
    void hitBeforeCutoffUsesIntraday() {
        Signal signal = signal("HIT", "22-05-2026 13:29:59");

        assertThat(resolver.resolveForOrder(signal))
                .isEqualTo(BrokerProductType.INTRADAY);
    }

    @Test
    void hitAtCutoffUsesCnc() {
        Signal signal = signal("HIT", "22-05-2026 13:30:00");

        assertThat(resolver.resolveForOrder(signal))
                .isEqualTo(BrokerProductType.CNC);
    }

    @Test
    void nonHitTradeTypesUseCnc() {
        assertThat(resolver.resolveForOrder(signal("DIT", "22-05-2026 09:15:00")))
                .isEqualTo(BrokerProductType.CNC);
        assertThat(resolver.resolveForOrder(signal("WIT", "22-05-2026 09:15:00")))
                .isEqualTo(BrokerProductType.CNC);
        assertThat(resolver.resolveForOrder(signal("MIT", "22-05-2026 09:15:00")))
                .isEqualTo(BrokerProductType.CNC);
        assertThat(resolver.resolveForOrder(signal("QIT", "22-05-2026 09:15:00")))
                .isEqualTo(BrokerProductType.CNC);
        assertThat(resolver.resolveForOrder(signal("HYIT", "22-05-2026 09:15:00")))
                .isEqualTo(BrokerProductType.CNC);
    }

    @Test
    void persistentOrdersAlwaysUseCnc() {
        Signal signal = signal("HIT", "22-05-2026 10:00:00");

        assertThat(resolver.resolveForPersistentOrder(signal))
                .isEqualTo(BrokerProductType.CNC);
    }

    private Signal signal(
            String tradeType,
            String alertDateTimeStamp) {

        Signal signal = new Signal();
        signal.setTradeType(tradeType);
        signal.setAlertDateTimeStamp(alertDateTimeStamp);
        return signal;
    }
}
