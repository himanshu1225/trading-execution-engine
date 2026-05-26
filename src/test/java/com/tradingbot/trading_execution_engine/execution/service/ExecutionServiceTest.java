package com.tradingbot.trading_execution_engine.execution.service;

import com.tradingbot.trading_execution_engine.broker.model.BrokerOrderStatus;
import com.tradingbot.trading_execution_engine.broker.model.OrderResponse;
import com.tradingbot.trading_execution_engine.broker.model.PersistentOrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;
import com.tradingbot.trading_execution_engine.broker.service.BrokerOrderService;
import com.tradingbot.trading_execution_engine.decision.model.TradeDecision;
import com.tradingbot.trading_execution_engine.order.model.OrderLegStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderSide;
import com.tradingbot.trading_execution_engine.order.model.OrderStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.OrderLeg;
import com.tradingbot.trading_execution_engine.persistence.entity.Signal;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderLegRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @Mock
    private BrokerOrderService brokerOrderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLegRepository orderLegRepository;

    private ExecutionService executionService;

    private final ExecutionProductResolver executionProductResolver =
            new ExecutionProductResolver();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        executionService =
                new ExecutionService(
                        brokerOrderService,
                        orderRepository,
                        orderLegRepository,
                        executionProductResolver
                );
    }

    @Test
    void executePlacesLimitOrderAndPersistsPendingOrder() {
        Signal signal = new Signal();
        signal.setSymbol("RELIANCE");
        signal.setStopLossPrice(2770.0);
        signal.setTradeType("HIT");
        signal.setAlertDateTimeStamp("22-05-2026 13:29:00");

        TradeDecision decision = TradeDecision.builder()
                .valid(true)
                .actionType(OrderType.LIMIT.name())
                .actualEntryPrice(2800.0)
                .quantity(10)
                .targetPrice(2860.0)
                .trailingJump(14.0)
                .build();

        when(brokerOrderService.placeSuperOrder(org.mockito.ArgumentMatchers.any()))
                .thenReturn(OrderResponse.builder()
                        .brokerOrderId("SUPER-1")
                        .securityId("2885")
                        .exchangeSegment("NSE_EQ")
                        .status(BrokerOrderStatus.PLACED)
                        .build());
        when(orderRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        executionService.execute(signal, decision);

        ArgumentCaptor<SuperOrderRequest> requestCaptor =
                ArgumentCaptor.forClass(SuperOrderRequest.class);
        verify(brokerOrderService).placeSuperOrder(requestCaptor.capture());

        SuperOrderRequest request = requestCaptor.getValue();
        assertThat(request.getSymbol()).isEqualTo("RELIANCE");
        assertThat(request.getSide()).isEqualTo(OrderSide.BUY);
        assertThat(request.getEntryOrderType()).isEqualTo(OrderType.LIMIT);
        assertThat(request.getProductType().name()).isEqualTo("INTRADAY");
        assertThat(request.getQuantity()).isEqualTo(10);
        assertThat(request.getPrice()).isEqualTo(2800.0);
        assertThat(request.getStopLossPrice()).isEqualTo(2770.0);
        assertThat(request.getTargetPrice()).isEqualTo(2860.0);
        assertThat(request.getTrailingJump()).isEqualTo(14.0);

        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order order = orderCaptor.getValue();
        assertThat(order.getBrokerOrderId()).isEqualTo("SUPER-1");
        assertThat(order.getSymbol()).isEqualTo("RELIANCE");
        assertThat(order.getSecurityId()).isEqualTo("2885");
        assertThat(order.getExchangeSegment()).isEqualTo("NSE_EQ");
        assertThat(order.getOrderType()).isEqualTo(OrderType.SUPER_ORDER.name());
        assertThat(order.getProductType()).isEqualTo("INTRADAY");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(order.getOrderPrice()).isEqualTo(2800.0);
        assertThat(order.getStopLossPrice()).isEqualTo(2770.0);
        assertThat(order.getTargetPrice()).isEqualTo(2860.0);
        assertThat(order.getTrailingJump()).isEqualTo(14.0);
        assertThat(order.getQuantity()).isEqualTo(10);
        assertThat(order.getSignal()).isSameAs(signal);

        ArgumentCaptor<OrderLeg> legCaptor =
                ArgumentCaptor.forClass(OrderLeg.class);
        verify(orderLegRepository, times(3)).save(legCaptor.capture());

        assertThat(legCaptor.getAllValues())
                .extracting(OrderLeg::getLegName)
                .containsExactly(
                        SuperOrderLeg.ENTRY_LEG.name(),
                        SuperOrderLeg.TARGET_LEG.name(),
                        SuperOrderLeg.STOP_LOSS_LEG.name()
                );
        assertThat(legCaptor.getAllValues())
                .allSatisfy(leg -> {
                    assertThat(leg.getBrokerOrderId()).isEqualTo("SUPER-1");
                    assertThat(leg.getLegStatus()).isEqualTo(OrderLegStatus.PENDING.name());
                    assertThat(leg.getOrder()).isSameAs(order);
                });
    }

    @Test
    void createPersistentLimitOrderPlacesForeverOrderAndPersistsForeverActiveOrder() {
        Signal signal = new Signal();
        signal.setSymbol("TCS");
        signal.setEntryPrice(3900.0);
        signal.setStopLossPrice(3850.0);
        signal.setTargetPrice(4000.0);
        signal.setTrailingJump(19.5);
        signal.setQuantity(20);
        signal.setTradeType("HIT");
        signal.setAlertDateTimeStamp("22-05-2026 12:30:00");

        when(brokerOrderService.placePersistentOrder(org.mockito.ArgumentMatchers.any()))
                .thenReturn(OrderResponse.builder()
                        .brokerOrderId("GTT-1")
                        .securityId("11536")
                        .exchangeSegment("NSE_EQ")
                        .status(BrokerOrderStatus.PLACED)
                        .build());

        executionService.createPersistentLimitOrder(signal);

        ArgumentCaptor<PersistentOrderRequest> requestCaptor =
                ArgumentCaptor.forClass(PersistentOrderRequest.class);
        verify(brokerOrderService).placePersistentOrder(requestCaptor.capture());

        PersistentOrderRequest request = requestCaptor.getValue();
        assertThat(request.getSymbol()).isEqualTo("TCS");
        assertThat(request.getSide()).isEqualTo(OrderSide.BUY);
        assertThat(request.getProductType().name()).isEqualTo("CNC");
        assertThat(request.getQuantity()).isEqualTo(20);
        assertThat(request.getTriggerPrice()).isEqualTo(3900.0);
        assertThat(request.getLimitPrice()).isEqualTo(3900.0);
        assertThat(request.getStopLossPrice()).isEqualTo(3850.0);

        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order order = orderCaptor.getValue();
        assertThat(order.getBrokerOrderId()).isEqualTo("GTT-1");
        assertThat(order.getSecurityId()).isEqualTo("11536");
        assertThat(order.getExchangeSegment()).isEqualTo("NSE_EQ");
        assertThat(order.getOrderType()).isEqualTo(OrderType.PERSISTENT_LIMIT.name());
        assertThat(order.getProductType()).isEqualTo("CNC");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FOREVER_ACTIVE.name());
        assertThat(order.getOrderPrice()).isEqualTo(3900.0);
        assertThat(order.getStopLossPrice()).isEqualTo(3850.0);
        assertThat(order.getTargetPrice()).isEqualTo(4000.0);
        assertThat(order.getTrailingJump()).isEqualTo(19.5);
        assertThat(order.getQuantity()).isEqualTo(20);
        assertThat(order.getSignal()).isSameAs(signal);
    }
}
