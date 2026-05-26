package com.tradingbot.trading_execution_engine.broker.mock;

import com.tradingbot.trading_execution_engine.broker.model.MarginCheckRequest;
import com.tradingbot.trading_execution_engine.broker.model.MarginCheckResponse;
import com.tradingbot.trading_execution_engine.broker.service.BrokerMarginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!dhan")
@Slf4j
public class MockBrokerMarginService implements BrokerMarginService {

    @Override
    public MarginCheckResponse checkMargin(MarginCheckRequest request) {
        log.info(
                "Mock margin check passed symbol={}, product={}, qty={}, price={}",
                request.getSymbol(),
                request.getProductType(),
                request.getQuantity(),
                request.getPrice()
        );

        return MarginCheckResponse.builder()
                .totalMargin(0.0)
                .availableBalance(Double.MAX_VALUE)
                .insufficientBalance(0.0)
                .message("Mock margin check passed")
                .build();
    }
}
