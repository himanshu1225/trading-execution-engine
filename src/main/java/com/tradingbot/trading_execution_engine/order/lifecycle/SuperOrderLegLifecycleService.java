package com.tradingbot.trading_execution_engine.order.lifecycle;

import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;
import com.tradingbot.trading_execution_engine.broker.service.BrokerOrderService;
import com.tradingbot.trading_execution_engine.order.model.OrderLegStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.OrderLeg;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderLegRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperOrderLegLifecycleService {

    private final OrderRepository orderRepository;
    private final OrderLegRepository orderLegRepository;
    private final BrokerOrderService brokerOrderService;

    @Transactional
    public int cancelPendingExitLegs(Long orderId) {
        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Order not found: " + orderId
                        ));

        if (!OrderType.SUPER_ORDER.name().equals(order.getOrderType())) {
            throw new IllegalArgumentException(
                    "Order is not a super order: " + orderId
            );
        }

        List<OrderLeg> exitLegs =
                orderLegRepository.findByOrderAndLegNameInAndLegStatusIn(
                        order,
                        OrderLifecycleRules.EXIT_LEG_NAMES,
                        OrderLifecycleRules.PENDING_LEG_STATUSES
                );

        for (OrderLeg leg : exitLegs) {
            SuperOrderLeg superOrderLeg =
                    SuperOrderLeg.valueOf(leg.getLegName());

            brokerOrderService.cancelSuperOrderLeg(
                    order.getBrokerOrderId(),
                    superOrderLeg
            );

            leg.setLegStatus(OrderLegStatus.CANCELLED.name());
            leg.setCancelledAt(LocalDateTime.now());
            orderLegRepository.save(leg);

            log.info(
                    "Cancelled super order exit leg orderId={}, brokerOrderId={}, leg={}",
                    order.getId(),
                    order.getBrokerOrderId(),
                    superOrderLeg
            );
        }

        return exitLegs.size();
    }
}
