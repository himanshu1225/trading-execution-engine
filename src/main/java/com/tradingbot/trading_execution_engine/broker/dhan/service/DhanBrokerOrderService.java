package com.tradingbot.trading_execution_engine.broker.dhan.service;

import com.tradingbot.trading_execution_engine.broker.dhan.client.DhanBrokerClient;
import com.tradingbot.trading_execution_engine.broker.dhan.config.DhanBrokerProperties;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanForeverOrderRequest;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanOrderStatusResponse;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanPlaceOrderRequest;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanPlaceOrderResponse;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanSuperOrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.BrokerOrderStatus;
import com.tradingbot.trading_execution_engine.broker.model.BrokerProductType;
import com.tradingbot.trading_execution_engine.broker.model.OrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.OrderResponse;
import com.tradingbot.trading_execution_engine.broker.model.OrderStatusResponse;
import com.tradingbot.trading_execution_engine.broker.model.PersistentOrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;
import com.tradingbot.trading_execution_engine.broker.service.BrokerOrderService;
import com.tradingbot.trading_execution_engine.marketdata.model.InstrumentInfo;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("dhan")
@RequiredArgsConstructor
@Slf4j
public class DhanBrokerOrderService implements BrokerOrderService {

    private final DhanBrokerClient dhanBrokerClient;
    private final DhanBrokerProperties properties;
    private final DhanInstrumentResolverService instrumentResolverService;

    @Override
    public OrderResponse placeOrder(OrderRequest request) {
        InstrumentInfo instrument =
                instrumentResolverService.resolve(request.getSymbol());

        DhanPlaceOrderRequest dhanRequest =
                DhanPlaceOrderRequest.builder()
                        .dhanClientId(properties.getClientId())
                        .correlationId(correlationId("ORD"))
                        .transactionType(request.getSide().name())
                        .exchangeSegment(instrument.getExchangeSegment())
                        .productType(productTypeOrDefault(
                                request.getProductType(),
                                properties.getOrderProductType()
                        ))
                        .orderType(toDhanOrderType(request.getOrderType()))
                        .validity(properties.getOrderValidity())
                        .securityId(instrument.getSecurityId())
                        .quantity(request.getQuantity())
                        .price(priceOrZero(request))
                        .triggerPrice(0.0)
                        .afterMarketOrder(false)
                        .build();

        DhanPlaceOrderResponse dhanResponse =
                dhanBrokerClient.placeOrder(dhanRequest);

        log.info(
                "Dhan order placed id={}, status={}, symbol={}",
                dhanResponse.getOrderId(),
                dhanResponse.getOrderStatus(),
                request.getSymbol()
        );

        return OrderResponse.builder()
                .brokerOrderId(dhanResponse.getOrderId())
                .securityId(instrument.getSecurityId())
                .exchangeSegment(instrument.getExchangeSegment())
                .status(mapStatus(dhanResponse.getOrderStatus()))
                .message(dhanResponse.getOrderStatus())
                .build();
    }

    @Override
    public OrderResponse placePersistentOrder(PersistentOrderRequest request) {
        InstrumentInfo instrument =
                instrumentResolverService.resolve(request.getSymbol());

        DhanForeverOrderRequest dhanRequest =
                DhanForeverOrderRequest.builder()
                        .dhanClientId(properties.getClientId())
                        .correlationId(correlationId("GTT"))
                        .orderFlag("SINGLE")
                        .transactionType(request.getSide().name())
                        .exchangeSegment(instrument.getExchangeSegment())
                        .productType(productTypeOrDefault(
                                request.getProductType(),
                                BrokerProductType.CNC.name()
                        ))
                        .orderType("LIMIT")
                        .validity(properties.getForeverValidity())
                        .securityId(instrument.getSecurityId())
                        .quantity(request.getQuantity())
                        .price(request.getLimitPrice())
                        .triggerPrice(request.getTriggerPrice())
                        .build();

        DhanPlaceOrderResponse dhanResponse =
                dhanBrokerClient.placeForeverOrder(dhanRequest);

        log.info(
                "Dhan forever order placed id={}, status={}, symbol={}",
                dhanResponse.getOrderId(),
                dhanResponse.getOrderStatus(),
                request.getSymbol()
        );

        return OrderResponse.builder()
                .brokerOrderId(dhanResponse.getOrderId())
                .securityId(instrument.getSecurityId())
                .exchangeSegment(instrument.getExchangeSegment())
                .status(mapStatus(dhanResponse.getOrderStatus()))
                .message(dhanResponse.getOrderStatus())
                .build();
    }

    @Override
    public OrderResponse placeSuperOrder(SuperOrderRequest request) {
        InstrumentInfo instrument =
                instrumentResolverService.resolve(request.getSymbol());

        DhanSuperOrderRequest dhanRequest =
                DhanSuperOrderRequest.builder()
                        .dhanClientId(properties.getClientId())
                        .correlationId(correlationId("SUP"))
                        .transactionType(request.getSide().name())
                        .exchangeSegment(instrument.getExchangeSegment())
                        .productType(productTypeOrDefault(
                                request.getProductType(),
                                properties.getOrderProductType()
                        ))
                        .orderType(toDhanOrderType(request.getEntryOrderType()))
                        .securityId(instrument.getSecurityId())
                        .quantity(request.getQuantity())
                        .price(priceOrZero(request.getEntryOrderType(), request.getPrice()))
                        .targetPrice(request.getTargetPrice())
                        .stopLossPrice(request.getStopLossPrice())
                        .trailingJump(request.getTrailingJump())
                        .build();

        DhanPlaceOrderResponse dhanResponse =
                dhanBrokerClient.placeSuperOrder(dhanRequest);

        log.info(
                "Dhan super order placed id={}, status={}, symbol={}",
                dhanResponse.getOrderId(),
                dhanResponse.getOrderStatus(),
                request.getSymbol()
        );

        return OrderResponse.builder()
                .brokerOrderId(dhanResponse.getOrderId())
                .securityId(instrument.getSecurityId())
                .exchangeSegment(instrument.getExchangeSegment())
                .status(mapStatus(dhanResponse.getOrderStatus()))
                .message(dhanResponse.getOrderStatus())
                .build();
    }

    @Override
    public OrderStatusResponse getOrderStatus(String brokerOrderId) {
        DhanOrderStatusResponse dhanResponse =
                dhanBrokerClient.getOrderStatus(brokerOrderId);

        return OrderStatusResponse.builder()
                .brokerOrderId(dhanResponse.getOrderId())
                .status(mapStatus(dhanResponse.getOrderStatus()))
                .filledQuantity(dhanResponse.getFilledQty())
                .averagePrice(dhanResponse.getAverageTradedPrice())
                .message(dhanResponse.getOmsErrorDescription())
                .build();
    }

    @Override
    public void cancelOrder(String brokerOrderId) {
        DhanPlaceOrderResponse dhanResponse =
                dhanBrokerClient.cancelOrder(brokerOrderId);

        log.info(
                "Dhan order cancel requested id={}, status={}",
                dhanResponse.getOrderId(),
                dhanResponse.getOrderStatus()
        );
    }

    @Override
    public void cancelSuperOrderLeg(
            String brokerOrderId,
            SuperOrderLeg leg) {

        DhanPlaceOrderResponse dhanResponse =
                dhanBrokerClient.cancelSuperOrderLeg(
                        brokerOrderId,
                        leg.name()
                );

        log.info(
                "Dhan super order leg cancel requested id={}, leg={}, status={}",
                dhanResponse.getOrderId(),
                leg,
                dhanResponse.getOrderStatus()
        );
    }

    private String toDhanOrderType(OrderType orderType) {
        if (OrderType.MARKET.equals(orderType)) {
            return "MARKET";
        }

        return "LIMIT";
    }

    private Double priceOrZero(OrderRequest request) {
        return priceOrZero(
                request.getOrderType(),
                request.getPrice()
        );
    }

    private Double priceOrZero(
            OrderType orderType,
            Double price) {

        if (OrderType.MARKET.equals(orderType)) {
            return 0.0;
        }

        return price;
    }

    private String productTypeOrDefault(
            BrokerProductType productType,
            String defaultProductType) {

        if (productType == null) {
            return defaultProductType;
        }

        return productType.name();
    }

    private BrokerOrderStatus mapStatus(String dhanStatus) {
        if (dhanStatus == null) {
            return BrokerOrderStatus.FAILED;
        }

        return switch (dhanStatus.trim().toUpperCase()) {
            case "TRANSIT", "PENDING", "CONFIRM" -> BrokerOrderStatus.PENDING;
            case "TRADED" -> BrokerOrderStatus.FILLED;
            case "PART_TRADED" -> BrokerOrderStatus.PARTIAL_FILLED;
            case "CANCELLED" -> BrokerOrderStatus.CANCELLED;
            case "REJECTED" -> BrokerOrderStatus.REJECTED;
            case "EXPIRED" -> BrokerOrderStatus.EXPIRED;
            default -> BrokerOrderStatus.FAILED;
        };
    }

    private String correlationId(String prefix) {
        return prefix + "-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 26);
    }
}
