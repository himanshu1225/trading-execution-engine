package com.tradingbot.trading_execution_engine.execution.service;

import com.tradingbot.trading_execution_engine.broker.model.BrokerProductType;
import com.tradingbot.trading_execution_engine.broker.model.MarginCheckRequest;
import com.tradingbot.trading_execution_engine.broker.model.MarginCheckResponse;
import com.tradingbot.trading_execution_engine.broker.service.BrokerMarginService;
import com.tradingbot.trading_execution_engine.broker.service.BrokerOrderService;
import com.tradingbot.trading_execution_engine.decision.model.TradeDecision;
import com.tradingbot.trading_execution_engine.order.model.SignalStatus;
import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.Signal;
import com.tradingbot.trading_execution_engine.broker.model.OrderResponse;
import com.tradingbot.trading_execution_engine.broker.model.PersistentOrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;
import com.tradingbot.trading_execution_engine.order.model.OrderSide;
import com.tradingbot.trading_execution_engine.order.model.OrderLegStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import com.tradingbot.trading_execution_engine.persistence.entity.OrderLeg;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderLegRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionService {

    private final BrokerOrderService brokerOrderService;
    private final BrokerMarginService brokerMarginService;
    private final OrderRepository orderRepository;
    private final OrderLegRepository orderLegRepository;
    private final SignalRepository signalRepository;
    private final ExecutionProductResolver executionProductResolver;

    @Transactional
    public void execute(
            Signal signal,
            TradeDecision decision) {

        validateExecutablePlan(signal, decision);

        OrderType orderType =
                OrderType.valueOf(decision.getActionType());

        BrokerProductType productType =
                executionProductResolver.resolveForOrder(signal);

        SuperOrderRequest request =
                SuperOrderRequest.builder()
                        .symbol(signal.getSymbol())
                        .side(OrderSide.BUY)
                        .entryOrderType(orderType)
                        .productType(productType)
                        .quantity(decision.getQuantity())
                        .price(decision.getActualEntryPrice())
                        .targetPrice(decision.getTargetPrice())
                        .stopLossPrice(signal.getStopLossPrice())
                        .trailingJump(decision.getTrailingJump())
                        .build();

        if (hasInsufficientMargin(signal, request)) {
            return;
        }

        OrderResponse response =
                brokerOrderService.placeSuperOrder(request);

        OrderStatus orderStatus =
                OrderType.MARKET.equals(orderType)
                        ? OrderStatus.PLACED
                        : OrderStatus.PENDING;

        Order order = new Order();

        order.setBrokerOrderId(response.getBrokerOrderId());
        order.setSymbol(signal.getSymbol());
        order.setSecurityId(response.getSecurityId());
        order.setExchangeSegment(response.getExchangeSegment());
        order.setOrderPrice(decision.getActualEntryPrice());
        order.setStopLossPrice(signal.getStopLossPrice());
        order.setTargetPrice(decision.getTargetPrice());
        order.setTrailingJump(decision.getTrailingJump());
        order.setQuantity(decision.getQuantity());
        order.setOrderType(OrderType.SUPER_ORDER.name());
        order.setProductType(productType.name());
        order.setOrderStatus(orderStatus.name());
        order.setCreatedAt(LocalDateTime.now());
        order.setPlacedAt(LocalDateTime.now());
        order.setSignal(signal);

        Order savedOrder =
                orderRepository.save(order);

        createSuperOrderLegs(
                savedOrder,
                response.getBrokerOrderId(),
                decision
        );
    }

    @Transactional
    public void createPersistentLimitOrder(Signal signal) {

        validatePersistentExecutablePlan(signal);

        BrokerProductType productType =
                executionProductResolver.resolveForPersistentOrder(signal);

        OrderResponse response =
                brokerOrderService.placePersistentOrder(
                        PersistentOrderRequest.builder()
                                .symbol(signal.getSymbol())
                                .side(OrderSide.BUY)
                                .productType(productType)
                                .quantity(signal.getQuantity())
                                .triggerPrice(signal.getEntryPrice())
                                .limitPrice(signal.getEntryPrice())
                                .stopLossPrice(signal.getStopLossPrice())
                                .build()
                );

        Order order = new Order();

        order.setBrokerOrderId(response.getBrokerOrderId());
        order.setSymbol(signal.getSymbol());
        order.setSecurityId(response.getSecurityId());
        order.setExchangeSegment(response.getExchangeSegment());
        order.setOrderPrice(signal.getEntryPrice());
        order.setStopLossPrice(signal.getStopLossPrice());
        order.setTargetPrice(signal.getTargetPrice());
        order.setTrailingJump(signal.getTrailingJump());
        order.setQuantity(signal.getQuantity());
        order.setOrderType(OrderType.PERSISTENT_LIMIT.name());
        order.setProductType(productType.name());
        order.setOrderStatus(OrderStatus.FOREVER_ACTIVE.name());
        order.setCreatedAt(LocalDateTime.now());
        order.setPlacedAt(LocalDateTime.now());
        order.setSignal(signal);

        orderRepository.save(order);
    }

    private void createSuperOrderLegs(
            Order order,
            String brokerOrderId,
            TradeDecision decision) {

        orderLegRepository.save(entryLeg(
                order,
                brokerOrderId,
                decision
        ));
        orderLegRepository.save(targetLeg(
                order,
                brokerOrderId,
                decision
        ));
        orderLegRepository.save(stopLossLeg(
                order,
                brokerOrderId,
                decision
        ));
    }

    private OrderLeg entryLeg(
            Order order,
            String brokerOrderId,
            TradeDecision decision) {

        OrderLeg leg =
                baseLeg(
                        order,
                        brokerOrderId,
                        SuperOrderLeg.ENTRY_LEG
                );

        leg.setPrice(decision.getActualEntryPrice());
        leg.setQuantity(decision.getQuantity());

        return leg;
    }

    private OrderLeg targetLeg(
            Order order,
            String brokerOrderId,
            TradeDecision decision) {

        OrderLeg leg =
                baseLeg(
                        order,
                        brokerOrderId,
                        SuperOrderLeg.TARGET_LEG
                );

        leg.setPrice(decision.getTargetPrice());
        leg.setQuantity(decision.getQuantity());

        return leg;
    }

    private OrderLeg stopLossLeg(
            Order order,
            String brokerOrderId,
            TradeDecision decision) {

        OrderLeg leg =
                baseLeg(
                        order,
                        brokerOrderId,
                        SuperOrderLeg.STOP_LOSS_LEG
                );

        leg.setPrice(order.getStopLossPrice());
        leg.setTrailingJump(decision.getTrailingJump());
        leg.setQuantity(decision.getQuantity());

        return leg;
    }

    private OrderLeg baseLeg(
            Order order,
            String brokerOrderId,
            SuperOrderLeg legName) {

        OrderLeg leg = new OrderLeg();

        leg.setOrder(order);
        leg.setBrokerOrderId(brokerOrderId);
        leg.setLegName(legName.name());
        leg.setLegStatus(OrderLegStatus.PENDING.name());
        leg.setCreatedAt(LocalDateTime.now());

        return leg;
    }

    private boolean hasInsufficientMargin(
            Signal signal,
            SuperOrderRequest request) {

        try {
            MarginCheckResponse margin =
                    brokerMarginService.checkMargin(
                            MarginCheckRequest.builder()
                                    .symbol(request.getSymbol())
                                    .side(request.getSide())
                                    .productType(request.getProductType())
                                    .quantity(request.getQuantity())
                                    .price(request.getPrice())
                                    .triggerPrice(0.0)
                                    .build()
                    );

            if (margin.hasInsufficientFunds()) {
                rejectForInsufficientMargin(signal, margin);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warn(
                    "Margin check failed, continuing with broker order placement signalId={}, symbol={}, reason={}",
                    signal.getId(),
                    signal.getSymbol(),
                    e.getMessage()
            );
            return false;
        }
    }

    private void rejectForInsufficientMargin(
            Signal signal,
            MarginCheckResponse margin) {

        String reason =
                "Insufficient funds for order. Required margin=" +
                        margin.getTotalMargin() +
                        ", available balance=" +
                        margin.getAvailableBalance() +
                        ", shortfall=" +
                        margin.getInsufficientBalance();

        signal.setStatus(SignalStatus.REJECTED.name());
        signal.setDecisionReason(reason);
        signalRepository.save(signal);

        log.warn(
                "Execution rejected due to insufficient funds signalId={}, symbol={}, requiredMargin={}, availableBalance={}, shortfall={}",
                signal.getId(),
                signal.getSymbol(),
                margin.getTotalMargin(),
                margin.getAvailableBalance(),
                margin.getInsufficientBalance()
        );
    }

    private void validateExecutablePlan(
            Signal signal,
            TradeDecision decision) {

        if (signal.getStopLossPrice() == null) {
            throw new IllegalStateException(
                    "Cannot execute trade without stop loss"
            );
        }

        if (decision.getTargetPrice() == null) {
            throw new IllegalStateException(
                    "Cannot execute trade without target price"
            );
        }

        if (decision.getTrailingJump() == null) {
            throw new IllegalStateException(
                    "Cannot execute trade without trailing jump"
            );
        }
    }

    private void validatePersistentExecutablePlan(Signal signal) {
        if (signal.getStopLossPrice() == null) {
            throw new IllegalStateException(
                    "Cannot create persistent trade without stop loss"
            );
        }

        if (signal.getTargetPrice() == null) {
            throw new IllegalStateException(
                    "Cannot create persistent trade without target price"
            );
        }

        if (signal.getTrailingJump() == null) {
            throw new IllegalStateException(
                    "Cannot create persistent trade without trailing jump"
            );
        }
    }
}
