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
import com.tradingbot.trading_execution_engine.marketdata.model.InstrumentInfo;
import com.tradingbot.trading_execution_engine.order.model.OrderSide;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DhanBrokerOrderServiceTest {

    @Mock
    private DhanBrokerClient dhanBrokerClient;

    @Mock
    private DhanInstrumentResolverService instrumentResolverService;

    private DhanBrokerOrderService dhanBrokerOrderService;

    @BeforeEach
    void setUp() {
        DhanBrokerProperties properties = new DhanBrokerProperties();
        ReflectionTestUtils.setField(properties, "clientId", "client-1");
        ReflectionTestUtils.setField(properties, "orderProductType", "INTRADAY");
        ReflectionTestUtils.setField(properties, "orderValidity", "DAY");
        ReflectionTestUtils.setField(properties, "foreverProductType", "CNC");
        ReflectionTestUtils.setField(properties, "foreverValidity", "DAY");

        dhanBrokerOrderService =
                new DhanBrokerOrderService(
                        dhanBrokerClient,
                        properties,
                        instrumentResolverService
                );
    }

    @Test
    void marketOrderUsesZeroPriceAndMapsTradedStatusToFilled() {
        when(instrumentResolverService.resolve("RELIANCE"))
                .thenReturn(new InstrumentInfo("2885", "NSE_EQ", "EQUITY"));

        DhanPlaceOrderResponse dhanResponse = new DhanPlaceOrderResponse();
        dhanResponse.setOrderId("dhan-1");
        dhanResponse.setOrderStatus("TRADED");

        when(dhanBrokerClient.placeOrder(org.mockito.ArgumentMatchers.any()))
                .thenReturn(dhanResponse);

        OrderResponse response =
                dhanBrokerOrderService.placeOrder(
                        OrderRequest.builder()
                                .symbol("RELIANCE")
                                .side(OrderSide.BUY)
                                .orderType(OrderType.MARKET)
                                .productType(BrokerProductType.INTRADAY)
                                .quantity(5)
                                .price(2800.0)
                                .build()
                );

        ArgumentCaptor<DhanPlaceOrderRequest> requestCaptor =
                ArgumentCaptor.forClass(DhanPlaceOrderRequest.class);
        verify(dhanBrokerClient).placeOrder(requestCaptor.capture());

        DhanPlaceOrderRequest request = requestCaptor.getValue();
        assertThat(request.getDhanClientId()).isEqualTo("client-1");
        assertThat(request.getTransactionType()).isEqualTo("BUY");
        assertThat(request.getExchangeSegment()).isEqualTo("NSE_EQ");
        assertThat(request.getProductType()).isEqualTo("INTRADAY");
        assertThat(request.getOrderType()).isEqualTo("MARKET");
        assertThat(request.getValidity()).isEqualTo("DAY");
        assertThat(request.getSecurityId()).isEqualTo("2885");
        assertThat(request.getQuantity()).isEqualTo(5);
        assertThat(request.getPrice()).isEqualTo(0.0);
        assertThat(request.getTriggerPrice()).isEqualTo(0.0);
        assertThat(request.getAfterMarketOrder()).isFalse();
        assertThat(request.getCorrelationId()).startsWith("ORD-");

        assertThat(response.getBrokerOrderId()).isEqualTo("dhan-1");
        assertThat(response.getSecurityId()).isEqualTo("2885");
        assertThat(response.getExchangeSegment()).isEqualTo("NSE_EQ");
        assertThat(response.getStatus()).isEqualTo(BrokerOrderStatus.FILLED);
    }

    @Test
    void persistentOrderPostsForeverLimitOrder() {
        when(instrumentResolverService.resolve("TCS"))
                .thenReturn(new InstrumentInfo("11536", "NSE_EQ", "EQUITY"));

        DhanPlaceOrderResponse dhanResponse = new DhanPlaceOrderResponse();
        dhanResponse.setOrderId("gtt-1");
        dhanResponse.setOrderStatus("PENDING");

        when(dhanBrokerClient.placeForeverOrder(org.mockito.ArgumentMatchers.any()))
                .thenReturn(dhanResponse);

        OrderResponse response =
                dhanBrokerOrderService.placePersistentOrder(
                        PersistentOrderRequest.builder()
                                .symbol("TCS")
                                .side(OrderSide.BUY)
                                .productType(BrokerProductType.CNC)
                                .quantity(3)
                                .triggerPrice(3900.0)
                                .limitPrice(3900.0)
                                .stopLossPrice(3850.0)
                                .build()
                );

        ArgumentCaptor<DhanForeverOrderRequest> requestCaptor =
                ArgumentCaptor.forClass(DhanForeverOrderRequest.class);
        verify(dhanBrokerClient).placeForeverOrder(requestCaptor.capture());

        DhanForeverOrderRequest request = requestCaptor.getValue();
        assertThat(request.getDhanClientId()).isEqualTo("client-1");
        assertThat(request.getCorrelationId()).startsWith("GTT-");
        assertThat(request.getOrderFlag()).isEqualTo("SINGLE");
        assertThat(request.getTransactionType()).isEqualTo("BUY");
        assertThat(request.getExchangeSegment()).isEqualTo("NSE_EQ");
        assertThat(request.getProductType()).isEqualTo("CNC");
        assertThat(request.getOrderType()).isEqualTo("LIMIT");
        assertThat(request.getValidity()).isEqualTo("DAY");
        assertThat(request.getSecurityId()).isEqualTo("11536");
        assertThat(request.getQuantity()).isEqualTo(3);
        assertThat(request.getPrice()).isEqualTo(3900.0);
        assertThat(request.getTriggerPrice()).isEqualTo(3900.0);

        assertThat(response.getBrokerOrderId()).isEqualTo("gtt-1");
        assertThat(response.getSecurityId()).isEqualTo("11536");
        assertThat(response.getExchangeSegment()).isEqualTo("NSE_EQ");
        assertThat(response.getStatus()).isEqualTo(BrokerOrderStatus.PENDING);
    }

    @Test
    void superOrderPostsEntryTargetStopLossAndTrailingJump() {
        when(instrumentResolverService.resolve("INFY"))
                .thenReturn(new InstrumentInfo("1594", "NSE_EQ", "EQUITY"));

        DhanPlaceOrderResponse dhanResponse = new DhanPlaceOrderResponse();
        dhanResponse.setOrderId("super-1");
        dhanResponse.setOrderStatus("PENDING");

        when(dhanBrokerClient.placeSuperOrder(org.mockito.ArgumentMatchers.any()))
                .thenReturn(dhanResponse);

        OrderResponse response =
                dhanBrokerOrderService.placeSuperOrder(
                        SuperOrderRequest.builder()
                                .symbol("INFY")
                                .side(OrderSide.BUY)
                                .entryOrderType(OrderType.LIMIT)
                                .productType(BrokerProductType.CNC)
                                .quantity(7)
                                .price(1500.0)
                                .targetPrice(1550.0)
                                .stopLossPrice(1475.0)
                                .trailingJump(7.5)
                                .build()
                );

        ArgumentCaptor<DhanSuperOrderRequest> requestCaptor =
                ArgumentCaptor.forClass(DhanSuperOrderRequest.class);
        verify(dhanBrokerClient).placeSuperOrder(requestCaptor.capture());

        DhanSuperOrderRequest request = requestCaptor.getValue();
        assertThat(request.getDhanClientId()).isEqualTo("client-1");
        assertThat(request.getCorrelationId()).startsWith("SUP-");
        assertThat(request.getTransactionType()).isEqualTo("BUY");
        assertThat(request.getExchangeSegment()).isEqualTo("NSE_EQ");
        assertThat(request.getProductType()).isEqualTo("CNC");
        assertThat(request.getOrderType()).isEqualTo("LIMIT");
        assertThat(request.getSecurityId()).isEqualTo("1594");
        assertThat(request.getQuantity()).isEqualTo(7);
        assertThat(request.getPrice()).isEqualTo(1500.0);
        assertThat(request.getTargetPrice()).isEqualTo(1550.0);
        assertThat(request.getStopLossPrice()).isEqualTo(1475.0);
        assertThat(request.getTrailingJump()).isEqualTo(7.5);

        assertThat(response.getBrokerOrderId()).isEqualTo("super-1");
        assertThat(response.getSecurityId()).isEqualTo("1594");
        assertThat(response.getExchangeSegment()).isEqualTo("NSE_EQ");
        assertThat(response.getStatus()).isEqualTo(BrokerOrderStatus.PENDING);
    }

    @Test
    void getOrderStatusMapsKnownAndUnknownDhanStatuses() {
        DhanOrderStatusResponse dhanResponse = new DhanOrderStatusResponse();
        dhanResponse.setOrderId("dhan-1");
        dhanResponse.setOrderStatus("PART_TRADED");
        dhanResponse.setFilledQty(2);
        dhanResponse.setAverageTradedPrice(101.5);
        dhanResponse.setOmsErrorDescription("partial");

        when(dhanBrokerClient.getOrderStatus("dhan-1"))
                .thenReturn(dhanResponse);

        OrderStatusResponse response =
                dhanBrokerOrderService.getOrderStatus("dhan-1");

        assertThat(response.getBrokerOrderId()).isEqualTo("dhan-1");
        assertThat(response.getStatus()).isEqualTo(BrokerOrderStatus.PARTIAL_FILLED);
        assertThat(response.getFilledQuantity()).isEqualTo(2);
        assertThat(response.getAveragePrice()).isEqualTo(101.5);
        assertThat(response.getMessage()).isEqualTo("partial");
    }

    @Test
    void cancelSuperOrderLegCallsDhanClientWithLegName() {
        DhanPlaceOrderResponse dhanResponse = new DhanPlaceOrderResponse();
        dhanResponse.setOrderId("super-1");
        dhanResponse.setOrderStatus("CANCELLED");

        when(dhanBrokerClient.cancelSuperOrderLeg(
                "super-1",
                "TARGET_LEG"
        )).thenReturn(dhanResponse);

        dhanBrokerOrderService.cancelSuperOrderLeg(
                "super-1",
                SuperOrderLeg.TARGET_LEG
        );

        verify(dhanBrokerClient).cancelSuperOrderLeg(
                "super-1",
                "TARGET_LEG"
        );
    }
}
