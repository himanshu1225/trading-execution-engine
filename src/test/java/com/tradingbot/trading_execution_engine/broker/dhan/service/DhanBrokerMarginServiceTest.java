package com.tradingbot.trading_execution_engine.broker.dhan.service;

import com.tradingbot.trading_execution_engine.broker.dhan.client.DhanBrokerClient;
import com.tradingbot.trading_execution_engine.broker.dhan.config.DhanBrokerProperties;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanMarginRequest;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanMarginResponse;
import com.tradingbot.trading_execution_engine.broker.model.BrokerProductType;
import com.tradingbot.trading_execution_engine.broker.model.MarginCheckRequest;
import com.tradingbot.trading_execution_engine.broker.model.MarginCheckResponse;
import com.tradingbot.trading_execution_engine.marketdata.model.InstrumentInfo;
import com.tradingbot.trading_execution_engine.order.model.OrderSide;
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
class DhanBrokerMarginServiceTest {

    @Mock
    private DhanBrokerClient dhanBrokerClient;

    @Mock
    private DhanInstrumentResolverService instrumentResolverService;

    private DhanBrokerMarginService dhanBrokerMarginService;

    @BeforeEach
    void setUp() {
        DhanBrokerProperties properties = new DhanBrokerProperties();
        ReflectionTestUtils.setField(properties, "clientId", "client-1");

        dhanBrokerMarginService =
                new DhanBrokerMarginService(
                        dhanBrokerClient,
                        properties,
                        instrumentResolverService
                );
    }

    @Test
    void checkMarginPostsDhanMarginRequestAndMapsResponse() {
        when(instrumentResolverService.resolve("RELIANCE"))
                .thenReturn(new InstrumentInfo("2885", "NSE_EQ", "EQUITY"));

        DhanMarginResponse dhanResponse = new DhanMarginResponse();
        dhanResponse.setTotalMargin(28000.0);
        dhanResponse.setAvailableBalance(25000.0);
        dhanResponse.setInsufficientBalance(3000.0);

        when(dhanBrokerClient.calculateMargin(org.mockito.ArgumentMatchers.any()))
                .thenReturn(dhanResponse);

        MarginCheckResponse response =
                dhanBrokerMarginService.checkMargin(
                        MarginCheckRequest.builder()
                                .symbol("RELIANCE")
                                .side(OrderSide.BUY)
                                .productType(BrokerProductType.CNC)
                                .quantity(10)
                                .price(2800.0)
                                .build()
                );

        ArgumentCaptor<DhanMarginRequest> requestCaptor =
                ArgumentCaptor.forClass(DhanMarginRequest.class);
        verify(dhanBrokerClient).calculateMargin(requestCaptor.capture());

        DhanMarginRequest request = requestCaptor.getValue();
        assertThat(request.getDhanClientId()).isEqualTo("client-1");
        assertThat(request.getExchangeSegment()).isEqualTo("NSE_EQ");
        assertThat(request.getTransactionType()).isEqualTo("BUY");
        assertThat(request.getQuantity()).isEqualTo(10);
        assertThat(request.getProductType()).isEqualTo("CNC");
        assertThat(request.getSecurityId()).isEqualTo("2885");
        assertThat(request.getPrice()).isEqualTo(2800.0);
        assertThat(request.getTriggerPrice()).isEqualTo(0.0);

        assertThat(response.getTotalMargin()).isEqualTo(28000.0);
        assertThat(response.getAvailableBalance()).isEqualTo(25000.0);
        assertThat(response.getInsufficientBalance()).isEqualTo(3000.0);
        assertThat(response.hasInsufficientFunds()).isTrue();
    }
}
