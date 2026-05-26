package com.tradingbot.trading_execution_engine.broker.dhan.service;

import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanOrderPostback;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;
import com.tradingbot.trading_execution_engine.order.model.OrderLegStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderStatus;
import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.OrderLeg;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderLegRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class DhanPostbackServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLegRepository orderLegRepository;

    private DhanPostbackService service;

    @BeforeEach
    void setUp() {
        service =
                new DhanPostbackService(
                        orderRepository,
                        orderLegRepository
                );
    }

    @Test
    void entryLegTradedMarksLegTradedAndOrderPlaced() {
        Order order =
                order();
        OrderLeg entryLeg =
                leg(order, SuperOrderLeg.ENTRY_LEG);
        DhanOrderPostback postback =
                postback("TRADED", SuperOrderLeg.ENTRY_LEG.name());

        when(orderRepository.findByBrokerOrderId("super-1"))
                .thenReturn(Optional.of(order));
        when(orderLegRepository.findByOrderAndLegName(
                order,
                SuperOrderLeg.ENTRY_LEG.name()
        )).thenReturn(Optional.of(entryLeg));

        service.process(postback);

        assertThat(entryLeg.getLegStatus()).isEqualTo(OrderLegStatus.TRADED.name());
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PLACED.name());
        verify(orderLegRepository).save(entryLeg);
        verify(orderRepository).save(order);
    }

    @Test
    void targetLegTradedMarksLegTradedAndOrderFilled() {
        Order order =
                order();
        OrderLeg targetLeg =
                leg(order, SuperOrderLeg.TARGET_LEG);
        DhanOrderPostback postback =
                postback("TRADED", SuperOrderLeg.TARGET_LEG.name());

        when(orderRepository.findByBrokerOrderId("super-1"))
                .thenReturn(Optional.of(order));
        when(orderLegRepository.findByOrderAndLegName(
                order,
                SuperOrderLeg.TARGET_LEG.name()
        )).thenReturn(Optional.of(targetLeg));

        service.process(postback);

        assertThat(targetLeg.getLegStatus()).isEqualTo(OrderLegStatus.TRADED.name());
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FILLED.name());
        assertThat(order.getFilledAt()).isNotNull();
        verify(orderLegRepository).save(targetLeg);
        verify(orderRepository).save(order);
    }

    @Test
    void unknownOrderIsIgnored() {
        DhanOrderPostback postback =
                postback("TRADED", SuperOrderLeg.ENTRY_LEG.name());

        when(orderRepository.findByBrokerOrderId("super-1"))
                .thenReturn(Optional.empty());

        service.process(postback);

        verify(orderLegRepository, never())
                .findByOrderAndLegName(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void missingLegIsCreatedFromPostback() {
        Order order =
                order();
        DhanOrderPostback postback =
                postback("TRADED", SuperOrderLeg.ENTRY_LEG.name());
        postback.setPrice(100.0);
        postback.setQuantity(10);

        when(orderRepository.findByBrokerOrderId("super-1"))
                .thenReturn(Optional.of(order));
        when(orderLegRepository.findByOrderAndLegName(
                order,
                SuperOrderLeg.ENTRY_LEG.name()
        )).thenReturn(Optional.empty());

        service.process(postback);

        ArgumentCaptor<OrderLeg> legCaptor =
                ArgumentCaptor.forClass(OrderLeg.class);
        verify(orderLegRepository).save(legCaptor.capture());

        OrderLeg createdLeg =
                legCaptor.getValue();

        assertThat(createdLeg.getOrder()).isSameAs(order);
        assertThat(createdLeg.getBrokerOrderId()).isEqualTo("super-1");
        assertThat(createdLeg.getLegName()).isEqualTo(SuperOrderLeg.ENTRY_LEG.name());
        assertThat(createdLeg.getLegStatus()).isEqualTo(OrderLegStatus.TRADED.name());
        assertThat(createdLeg.getPrice()).isEqualTo(100.0);
        assertThat(createdLeg.getQuantity()).isEqualTo(10);
        assertThat(createdLeg.getCreatedAt()).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PLACED.name());
        verify(orderRepository).save(order);
    }

    private Order order() {
        Order order = new Order();
        order.setId(1L);
        order.setBrokerOrderId("super-1");
        order.setOrderStatus(OrderStatus.PENDING.name());
        return order;
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

    private DhanOrderPostback postback(
            String orderStatus,
            String legName) {

        DhanOrderPostback postback = new DhanOrderPostback();
        postback.setOrderId("super-1");
        postback.setOrderStatus(orderStatus);
        postback.setLegName(legName);
        postback.setFilledQty(10);
        return postback;
    }
}
