package com.tradingbot.trading_execution_engine.broker.dhan.service;

import com.tradingbot.trading_execution_engine.broker.dhan.client.DhanBrokerClient;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanPositionResponse;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;
import com.tradingbot.trading_execution_engine.order.lifecycle.OrderLifecycleRules;
import com.tradingbot.trading_execution_engine.order.lifecycle.OrderReconciliationResult;
import com.tradingbot.trading_execution_engine.order.lifecycle.SuperOrderLegLifecycleService;
import com.tradingbot.trading_execution_engine.order.model.OrderLegStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.OrderLeg;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderLegRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DhanOrderReconciliationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLegRepository orderLegRepository;

    @Mock
    private DhanBrokerClient dhanBrokerClient;

    @Mock
    private SuperOrderLegLifecycleService superOrderLegLifecycleService;

    private DhanOrderReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService =
                new DhanOrderReconciliationService(
                        orderRepository,
                        orderLegRepository,
                        dhanBrokerClient,
                        superOrderLegLifecycleService
                );
    }

    @Test
    void reconcileDoesNotCancelExitLegsWhenDhanPositionIsStillOpen() {
        Order order = activeSuperOrder();

        when(orderRepository.findByOrderTypeAndOrderStatusIn(
                OrderType.SUPER_ORDER.name(),
                OrderLifecycleRules.ACTIVE_SUPER_ORDER_STATUSES
        )).thenReturn(List.of(order));
        when(dhanBrokerClient.getPositions())
                .thenReturn(List.of(openPosition()));
        when(orderLegRepository.findByOrderAndLegName(
                order,
                SuperOrderLeg.ENTRY_LEG.name()
        )).thenReturn(Optional.of(leg(SuperOrderLeg.ENTRY_LEG)));
        when(orderLegRepository.findByOrderAndLegNameInAndLegStatusIn(
                order,
                OrderLifecycleRules.EXIT_LEG_NAMES,
                OrderLifecycleRules.PENDING_LEG_STATUSES
        )).thenReturn(List.of(
                leg(SuperOrderLeg.TARGET_LEG),
                leg(SuperOrderLeg.STOP_LOSS_LEG)
        ));

        OrderReconciliationResult result =
                reconciliationService.reconcileManualSquareOffs();

        assertThat(result.getCheckedOrders()).isEqualTo(1);
        assertThat(result.getSkippedOrders()).isEqualTo(0);
        assertThat(result.getClosedPositionsDetected()).isEqualTo(0);
        assertThat(result.getCancelledExitLegs()).isEqualTo(0);

        verify(superOrderLegLifecycleService, never())
                .cancelPendingExitLegs(order.getId());
    }

    @Test
    void reconcileCancelsPendingExitLegsWhenDhanPositionIsClosed() {
        Order order = activeSuperOrder();

        when(orderRepository.findByOrderTypeAndOrderStatusIn(
                OrderType.SUPER_ORDER.name(),
                OrderLifecycleRules.ACTIVE_SUPER_ORDER_STATUSES
        )).thenReturn(List.of(order));
        when(dhanBrokerClient.getPositions())
                .thenReturn(List.of(closedPosition()));
        when(orderLegRepository.findByOrderAndLegName(
                order,
                SuperOrderLeg.ENTRY_LEG.name()
        )).thenReturn(Optional.of(leg(SuperOrderLeg.ENTRY_LEG)));
        when(orderLegRepository.findByOrderAndLegNameInAndLegStatusIn(
                order,
                OrderLifecycleRules.EXIT_LEG_NAMES,
                OrderLifecycleRules.PENDING_LEG_STATUSES
        )).thenReturn(List.of(
                leg(SuperOrderLeg.TARGET_LEG),
                leg(SuperOrderLeg.STOP_LOSS_LEG)
        ));
        when(superOrderLegLifecycleService.cancelPendingExitLegs(order.getId()))
                .thenReturn(2);

        OrderReconciliationResult result =
                reconciliationService.reconcileManualSquareOffs();

        assertThat(result.getCheckedOrders()).isEqualTo(1);
        assertThat(result.getSkippedOrders()).isEqualTo(0);
        assertThat(result.getClosedPositionsDetected()).isEqualTo(1);
        assertThat(result.getCancelledExitLegs()).isEqualTo(2);

        verify(superOrderLegLifecycleService)
                .cancelPendingExitLegs(order.getId());
    }

    private Order activeSuperOrder() {
        Order order = new Order();

        order.setId(10L);
        order.setBrokerOrderId("super-1");
        order.setOrderType(OrderType.SUPER_ORDER.name());
        order.setSecurityId("11536");
        order.setExchangeSegment("NSE_EQ");
        order.setProductType("CNC");

        return order;
    }

    private OrderLeg leg(SuperOrderLeg legName) {
        OrderLeg leg = new OrderLeg();

        leg.setLegName(legName.name());
        leg.setLegStatus(OrderLegStatus.TRADED.name());

        if (!SuperOrderLeg.ENTRY_LEG.equals(legName)) {
            leg.setLegStatus(OrderLegStatus.PENDING.name());
        }

        return leg;
    }

    private DhanPositionResponse openPosition() {
        DhanPositionResponse position = basePosition();

        position.setPositionType("LONG");
        position.setNetQty(10);

        return position;
    }

    private DhanPositionResponse closedPosition() {
        DhanPositionResponse position = basePosition();

        position.setPositionType("CLOSED");
        position.setNetQty(0);

        return position;
    }

    private DhanPositionResponse basePosition() {
        DhanPositionResponse position = new DhanPositionResponse();

        position.setSecurityId("11536");
        position.setExchangeSegment("NSE_EQ");
        position.setProductType("CNC");

        return position;
    }
}
