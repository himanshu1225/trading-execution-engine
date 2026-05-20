package com.tradingbot.trading_execution_engine.broker.mock;

import com.tradingbot.trading_execution_engine.broker.model.BrokerOrderStatus;
import com.tradingbot.trading_execution_engine.broker.model.OrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.OrderResponse;
import com.tradingbot.trading_execution_engine.broker.model.OrderStatusResponse;
import com.tradingbot.trading_execution_engine.broker.model.PersistentOrderRequest;
import com.tradingbot.trading_execution_engine.broker.service.BrokerOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("!dhan")
@Slf4j
public class MockBrokerOrderService implements BrokerOrderService {

    @Override
    public OrderResponse placeOrder(OrderRequest request) {

        String brokerOrderId =
                request.getOrderType().name() + "-" + UUID.randomUUID();

        log.info(
                "Mock broker order placed id={}, symbol={}, type={}, qty={}, price={}",
                brokerOrderId,
                request.getSymbol(),
                request.getOrderType(),
                request.getQuantity(),
                request.getPrice()
        );

        return OrderResponse.builder()
                .brokerOrderId(brokerOrderId)
                .status(BrokerOrderStatus.PLACED)
                .message("Mock order placed")
                .build();
    }

    @Override
    public OrderResponse placePersistentOrder(PersistentOrderRequest request) {

        String brokerOrderId =
                "PERSISTENT-" + UUID.randomUUID();

        log.info(
                "Mock persistent order placed id={}, symbol={}, qty={}, trigger={}, limit={}",
                brokerOrderId,
                request.getSymbol(),
                request.getQuantity(),
                request.getTriggerPrice(),
                request.getLimitPrice()
        );

        return OrderResponse.builder()
                .brokerOrderId(brokerOrderId)
                .status(BrokerOrderStatus.PLACED)
                .message("Mock persistent order placed")
                .build();
    }

    @Override
    public OrderStatusResponse getOrderStatus(String brokerOrderId) {

        return OrderStatusResponse.builder()
                .brokerOrderId(brokerOrderId)
                .status(BrokerOrderStatus.EXPIRED)
                .message("Mock order expired")
                .build();
    }

    @Override
    public void cancelOrder(String brokerOrderId) {
        log.info("Mock broker order cancelled id={}", brokerOrderId);
    }
}
