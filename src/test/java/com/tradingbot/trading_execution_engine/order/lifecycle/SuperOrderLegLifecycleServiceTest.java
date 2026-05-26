package com.tradingbot.trading_execution_engine.order.lifecycle;

import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;
import com.tradingbot.trading_execution_engine.broker.service.BrokerOrderService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperOrderLegLifecycleServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLegRepository orderLegRepository;

    @Mock
    private BrokerOrderService brokerOrderService;

    private SuperOrderLegLifecycleService service;

    @BeforeEach
    void setUp() {
        service =
                new SuperOrderLegLifecycleService(
                        orderRepository,
                        orderLegRepository,
                        brokerOrderService
                );
    }

    @Test
    void cancelsPendingTargetAndStopLossLegs() {
        Order order = new Order();
        order.setId(10L);
        order.setBrokerOrderId("super-1");
        order.setOrderType(OrderType.SUPER_ORDER.name());

        OrderLeg targetLeg =
                leg(order, SuperOrderLeg.TARGET_LEG);
        OrderLeg stopLossLeg =
                leg(order, SuperOrderLeg.STOP_LOSS_LEG);

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));
        when(orderLegRepository.findByOrderAndLegNameInAndLegStatusIn(
                order,
                List.of(
                        SuperOrderLeg.TARGET_LEG.name(),
                        SuperOrderLeg.STOP_LOSS_LEG.name()
                ),
                List.of(OrderLegStatus.PENDING.name())
        )).thenReturn(List.of(targetLeg, stopLossLeg));

        int cancelled =
                service.cancelPendingExitLegs(10L);

        assertThat(cancelled).isEqualTo(2);
        verify(brokerOrderService).cancelSuperOrderLeg(
                "super-1",
                SuperOrderLeg.TARGET_LEG
        );
        verify(brokerOrderService).cancelSuperOrderLeg(
                "super-1",
                SuperOrderLeg.STOP_LOSS_LEG
        );

        assertThat(targetLeg.getLegStatus()).isEqualTo(OrderLegStatus.CANCELLED.name());
        assertThat(stopLossLeg.getLegStatus()).isEqualTo(OrderLegStatus.CANCELLED.name());
        assertThat(targetLeg.getCancelledAt()).isNotNull();
        assertThat(stopLossLeg.getCancelledAt()).isNotNull();
        verify(orderLegRepository).save(targetLeg);
        verify(orderLegRepository).save(stopLossLeg);
    }

    private OrderLeg leg(
            Order order,
            SuperOrderLeg legName) {

        OrderLeg leg = new OrderLeg();
        leg.setOrder(order);
        leg.setBrokerOrderId(order.getBrokerOrderId());
        leg.setLegName(legName.name());
        leg.setLegStatus(OrderLegStatus.PENDING.name());
        return leg;
    }
}
