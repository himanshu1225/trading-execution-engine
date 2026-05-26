package com.tradingbot.trading_execution_engine.broker.dhan.service;

import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanOrderPostback;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;
import com.tradingbot.trading_execution_engine.order.model.OrderLegStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderStatus;
import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.OrderLeg;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderLegRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DhanPostbackService {

    private final OrderRepository orderRepository;
    private final OrderLegRepository orderLegRepository;

    @Transactional
    public void process(DhanOrderPostback postback) {
        if (postback.getOrderId() == null) {
            log.warn("Ignoring Dhan postback without orderId");
            return;
        }

        Optional<Order> maybeOrder =
                orderRepository.findByBrokerOrderId(
                        postback.getOrderId()
                );

        if (maybeOrder.isEmpty()) {
            log.warn(
                    "Ignoring Dhan postback for unknown brokerOrderId={}",
                    postback.getOrderId()
            );
            return;
        }

        Order order = maybeOrder.get();

        if (postback.getLegName() != null &&
                !postback.getLegName().isBlank()) {
            updateLeg(order, postback);
        }

        updateOrder(order, postback);
    }

    private void updateLeg(
            Order order,
            DhanOrderPostback postback) {

        String legName =
                postback.getLegName()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        OrderLeg leg =
                orderLegRepository.findByOrderAndLegName(
                                order,
                                legName
                        )
                        .orElseGet(() -> createMissingLeg(
                                order,
                                postback,
                                legName
                        ));

        leg.setLegStatus(toLegStatus(postback.getOrderStatus()));

        if (OrderLegStatus.CANCELLED.name().equals(leg.getLegStatus())) {
            leg.setCancelledAt(LocalDateTime.now());
        }

        orderLegRepository.save(leg);
    }

    private OrderLeg createMissingLeg(
            Order order,
            DhanOrderPostback postback,
            String legName) {

        log.warn(
                "Creating missing local order leg from Dhan postback orderId={}, brokerOrderId={}, legName={}",
                order.getId(),
                order.getBrokerOrderId(),
                legName
        );

        OrderLeg leg = new OrderLeg();

        leg.setOrder(order);
        leg.setBrokerOrderId(order.getBrokerOrderId());
        leg.setLegName(legName);
        leg.setLegStatus(OrderLegStatus.PENDING.name());
        leg.setPrice(resolveLegPrice(postback));
        leg.setQuantity(postback.getQuantity());
        leg.setCreatedAt(LocalDateTime.now());

        return leg;
    }

    private Double resolveLegPrice(DhanOrderPostback postback) {
        if (postback.getPrice() != null) {
            return postback.getPrice();
        }

        return postback.getTriggerPrice();
    }

    private void updateOrder(
            Order order,
            DhanOrderPostback postback) {

        String status =
                normalize(postback.getOrderStatus());

        String legName =
                normalize(postback.getLegName());

        if ("TRADED".equals(status) &&
                (SuperOrderLeg.TARGET_LEG.name().equals(legName) ||
                        SuperOrderLeg.STOP_LOSS_LEG.name().equals(legName))) {

            order.setOrderStatus(OrderStatus.FILLED.name());
            order.setFilledAt(LocalDateTime.now());

        } else if ("TRADED".equals(status) &&
                SuperOrderLeg.ENTRY_LEG.name().equals(legName)) {

            order.setOrderStatus(OrderStatus.PLACED.name());

        } else if ("CANCELLED".equals(status)) {
            order.setOrderStatus(OrderStatus.CANCELLED.name());
            order.setCancelledAt(LocalDateTime.now());

        } else if ("REJECTED".equals(status)) {
            order.setOrderStatus(OrderStatus.REJECTED.name());

        } else if ("EXPIRED".equals(status)) {
            order.setOrderStatus(OrderStatus.EXPIRED.name());
            order.setCancelledAt(LocalDateTime.now());
        }

        orderRepository.save(order);
    }

    private String toLegStatus(String brokerStatus) {
        String status =
                normalize(brokerStatus);

        return switch (status) {
            case "TRADED" -> OrderLegStatus.TRADED.name();
            case "CANCELLED" -> OrderLegStatus.CANCELLED.name();
            case "REJECTED" -> OrderLegStatus.REJECTED.name();
            default -> OrderLegStatus.PENDING.name();
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toUpperCase(Locale.ROOT);
    }
}
