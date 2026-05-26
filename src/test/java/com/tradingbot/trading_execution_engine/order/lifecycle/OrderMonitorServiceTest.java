package com.tradingbot.trading_execution_engine.order.lifecycle;

import com.tradingbot.trading_execution_engine.broker.model.BrokerOrderStatus;
import com.tradingbot.trading_execution_engine.broker.model.OrderResponse;
import com.tradingbot.trading_execution_engine.broker.model.OrderStatusResponse;
import com.tradingbot.trading_execution_engine.broker.service.BrokerOrderService;
import com.tradingbot.trading_execution_engine.execution.service.ExecutionProductResolver;
import com.tradingbot.trading_execution_engine.execution.service.ExecutionService;
import com.tradingbot.trading_execution_engine.order.model.OrderStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import com.tradingbot.trading_execution_engine.order.model.SignalStatus;
import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.Signal;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderLegRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.SignalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMonitorServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SignalRepository signalRepository;

    @Mock
    private BrokerOrderService brokerOrderService;

    @Mock
    private OrderLegRepository orderLegRepository;

    private OrderMonitorService orderMonitorService;

    @BeforeEach
    void setUp() {
        ExecutionService executionService =
                new ExecutionService(
                        brokerOrderService,
                        orderRepository,
                        orderLegRepository,
                        new ExecutionProductResolver()
                );
        orderMonitorService =
                new OrderMonitorService(
                        orderRepository,
                        signalRepository,
                        brokerOrderService,
                        executionService
                );
    }

    @Test
    void expiredPendingLimitOrderIsConvertedToPersistentLimitOrder() {
        Signal signal = new Signal();
        signal.setSymbol("INFY");
        signal.setEntryPrice(1500.0);
        signal.setStopLossPrice(1475.0);
        signal.setTargetPrice(1550.0);
        signal.setTrailingJump(7.5);
        signal.setQuantity(100);
        signal.setStatus(SignalStatus.PENDING_LIMIT.name());
        signal.setTradeType("HIT");
        signal.setAlertDateTimeStamp("22-05-2026 12:30:00");

        Order pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setBrokerOrderId("LIMIT-1");
        pendingOrder.setSymbol("INFY");
        pendingOrder.setOrderType(OrderType.LIMIT.name());
        pendingOrder.setOrderStatus(OrderStatus.PENDING.name());
        pendingOrder.setSignal(signal);

        when(orderRepository.findByOrderStatus(OrderStatus.PENDING.name()))
                .thenReturn(List.of(pendingOrder));
        when(brokerOrderService.getOrderStatus("LIMIT-1"))
                .thenReturn(OrderStatusResponse.builder()
                        .brokerOrderId("LIMIT-1")
                        .status(BrokerOrderStatus.EXPIRED)
                        .build());
        when(brokerOrderService.placePersistentOrder(org.mockito.ArgumentMatchers.any()))
                .thenReturn(OrderResponse.builder()
                        .brokerOrderId("GTT-1")
                        .status(BrokerOrderStatus.PLACED)
                        .build());

        orderMonitorService.monitorPendingOrders();

        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, org.mockito.Mockito.times(2)).save(orderCaptor.capture());

        List<Order> savedOrders = orderCaptor.getAllValues();
        Order persistentOrder = savedOrders.get(0);
        Order expiredOrder = savedOrders.get(1);

        assertThat(persistentOrder.getBrokerOrderId()).isEqualTo("GTT-1");
        assertThat(persistentOrder.getOrderType()).isEqualTo(OrderType.PERSISTENT_LIMIT.name());
        assertThat(persistentOrder.getProductType()).isEqualTo("CNC");
        assertThat(persistentOrder.getOrderStatus()).isEqualTo(OrderStatus.FOREVER_ACTIVE.name());
        assertThat(persistentOrder.getOrderPrice()).isEqualTo(1500.0);
        assertThat(persistentOrder.getStopLossPrice()).isEqualTo(1475.0);
        assertThat(persistentOrder.getTargetPrice()).isEqualTo(1550.0);
        assertThat(persistentOrder.getTrailingJump()).isEqualTo(7.5);
        assertThat(persistentOrder.getQuantity()).isEqualTo(100);
        assertThat(persistentOrder.getSignal()).isSameAs(signal);

        assertThat(expiredOrder).isSameAs(pendingOrder);
        assertThat(expiredOrder.getOrderStatus()).isEqualTo(OrderStatus.EXPIRED.name());
        assertThat(expiredOrder.getCancelledAt()).isNotNull();

        assertThat(signal.getStatus()).isEqualTo(SignalStatus.FOREVER_ACTIVE.name());
        verify(signalRepository).save(signal);
    }
}
