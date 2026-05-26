package com.tradingbot.trading_execution_engine.broker.dhan.service;

import com.tradingbot.trading_execution_engine.broker.dhan.client.DhanBrokerClient;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanPositionResponse;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;
import com.tradingbot.trading_execution_engine.order.lifecycle.OrderLifecycleRules;
import com.tradingbot.trading_execution_engine.order.lifecycle.OrderReconciliationResult;
import com.tradingbot.trading_execution_engine.order.lifecycle.OrderReconciliationService;
import com.tradingbot.trading_execution_engine.order.lifecycle.SuperOrderLegLifecycleService;
import com.tradingbot.trading_execution_engine.order.model.OrderLegStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.OrderLeg;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderLegRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("dhan")
@RequiredArgsConstructor
@Slf4j
public class DhanOrderReconciliationService implements OrderReconciliationService {

    private final OrderRepository orderRepository;
    private final OrderLegRepository orderLegRepository;
    private final DhanBrokerClient dhanBrokerClient;
    private final SuperOrderLegLifecycleService superOrderLegLifecycleService;

    @Override
    public OrderReconciliationResult reconcileManualSquareOffs() {
        List<Order> activeSuperOrders =
                findActiveSuperOrders();

        List<DhanPositionResponse> positions =
                dhanBrokerClient.getPositions();

        int checkedOrders = 0;
        int skippedOrders = 0;
        int closedPositionsDetected = 0;
        int cancelledExitLegs = 0;

        for (Order order : activeSuperOrders) {
            checkedOrders++;

            if (!isEligibleForReconciliation(order)) {
                skippedOrders++;
                continue;
            }

            if (hasOpenPosition(order, positions)) {
                continue;
            }

            int cancelled =
                    superOrderLegLifecycleService.cancelPendingExitLegs(
                            order.getId()
                    );

            if (cancelled > 0) {
                closedPositionsDetected++;
                cancelledExitLegs += cancelled;
                logClosedPosition(order);
            }
        }

        return OrderReconciliationResult.builder()
                .checkedOrders(checkedOrders)
                .skippedOrders(skippedOrders)
                .closedPositionsDetected(closedPositionsDetected)
                .cancelledExitLegs(cancelledExitLegs)
                .build();
    }

    private List<Order> findActiveSuperOrders() {
        return orderRepository.findByOrderTypeAndOrderStatusIn(
                OrderType.SUPER_ORDER.name(),
                OrderLifecycleRules.ACTIVE_SUPER_ORDER_STATUSES
        );
    }

    private boolean isEligibleForReconciliation(Order order) {
        if (order.getSecurityId() == null || order.getExchangeSegment() == null) {
            return false;
        }

        return entryLegTraded(order) && hasPendingExitLegs(order);
    }

    private boolean entryLegTraded(Order order) {
        return orderLegRepository.findByOrderAndLegName(
                        order,
                        SuperOrderLeg.ENTRY_LEG.name()
                )
                .map(OrderLeg::getLegStatus)
                .filter(OrderLegStatus.TRADED.name()::equals)
                .isPresent();
    }

    private boolean hasPendingExitLegs(Order order) {
        return !orderLegRepository.findByOrderAndLegNameInAndLegStatusIn(
                order,
                OrderLifecycleRules.EXIT_LEG_NAMES,
                OrderLifecycleRules.PENDING_LEG_STATUSES
        ).isEmpty();
    }

    private boolean hasOpenPosition(
            Order order,
            List<DhanPositionResponse> positions) {

        return positions.stream()
                .filter(position -> matchesOrder(order, position))
                .anyMatch(this::isOpenPosition);
    }

    private boolean matchesOrder(
            Order order,
            DhanPositionResponse position) {

        if (!order.getSecurityId().equals(position.getSecurityId())) {
            return false;
        }

        if (!order.getExchangeSegment().equals(position.getExchangeSegment())) {
            return false;
        }

        if (order.getProductType() == null || position.getProductType() == null) {
            return true;
        }

        return order.getProductType().equals(position.getProductType());
    }

    private boolean isOpenPosition(DhanPositionResponse position) {
        if ("CLOSED".equalsIgnoreCase(position.getPositionType())) {
            return false;
        }

        Integer netQty = position.getNetQty();

        return netQty != null && netQty != 0;
    }

    private void logClosedPosition(Order order) {
        log.warn(
                "Detected closed Dhan position and cancelled pending exit legs orderId={}, brokerOrderId={}, securityId={}, exchangeSegment={}",
                order.getId(),
                order.getBrokerOrderId(),
                order.getSecurityId(),
                order.getExchangeSegment()
        );
    }
}
