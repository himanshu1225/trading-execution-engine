package com.tradingbot.trading_execution_engine.marketdata.dhan;

import com.tradingbot.trading_execution_engine.broker.dhan.service.DhanInstrumentResolverService;
import com.tradingbot.trading_execution_engine.marketdata.dhan.dto.DhanIntradayRequest;
import com.tradingbot.trading_execution_engine.marketdata.dhan.dto.DhanIntradayResponse;
import com.tradingbot.trading_execution_engine.marketdata.dhan.dto.DhanLtpResponse;
import com.tradingbot.trading_execution_engine.marketdata.model.Candle;
import com.tradingbot.trading_execution_engine.marketdata.model.InstrumentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DhanMarketDataServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DhanInstrumentResolverService instrumentResolverService;

    private DhanMarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService =
                new DhanMarketDataService(
                        restTemplate,
                        instrumentResolverService
                );

        ReflectionTestUtils.setField(
                marketDataService,
                "dhanBaseUrl",
                "https://api.dhan.co/v2"
        );
        ReflectionTestUtils.setField(
                marketDataService,
                "accessToken",
                "token-1"
        );
        ReflectionTestUtils.setField(
                marketDataService,
                "clientId",
                "client-1"
        );
    }

    @Test
    void getCandlesAfterAlertPostsIntradayRequestAndMapsSortedCandles() {
        LocalDateTime alertTimestamp =
                LocalDateTime.of(2026, 5, 26, 10, 15);

        LocalDateTime beforeCutoff =
                alertTimestamp.minusMinutes(2);
        LocalDateTime atCutoff =
                alertTimestamp.minusMinutes(1);
        LocalDateTime afterAlert =
                alertTimestamp.plusMinutes(1);

        when(instrumentResolverService.resolve("RELIANCE"))
                .thenReturn(new InstrumentInfo("2885", "NSE_EQ", "EQUITY"));

        DhanIntradayResponse response = new DhanIntradayResponse();
        response.setOpen(List.of(100.0, 101.0, 102.0));
        response.setHigh(List.of(105.0, 106.0, 107.0));
        response.setLow(List.of(99.0, 100.0, 101.0));
        response.setClose(List.of(104.0, 105.0, 106.0));
        response.setTimestamp(List.of(
                epoch(beforeCutoff),
                epoch(afterAlert),
                epoch(atCutoff)
        ));

        when(restTemplate.postForEntity(
                eq("https://api.dhan.co/v2/charts/intraday"),
                org.mockito.ArgumentMatchers.any(HttpEntity.class),
                eq(DhanIntradayResponse.class)
        )).thenReturn(ResponseEntity.ok(response));

        List<Candle> candles =
                marketDataService.getCandlesAfterAlert(
                        "RELIANCE",
                        alertTimestamp
                );

        assertThat(candles).hasSize(2);
        assertThat(candles)
                .extracting(Candle::getTimestamp)
                .containsExactly(
                        atCutoff,
                        afterAlert
                );

        ArgumentCaptor<HttpEntity<DhanIntradayRequest>> entityCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("https://api.dhan.co/v2/charts/intraday"),
                entityCaptor.capture(),
                eq(DhanIntradayResponse.class)
        );

        DhanIntradayRequest request =
                entityCaptor.getValue().getBody();

        assertThat(request.getSecurityId()).isEqualTo("2885");
        assertThat(request.getExchangeSegment()).isEqualTo("NSE_EQ");
        assertThat(request.getInstrument()).isEqualTo("EQUITY");
        assertThat(request.getInterval()).isEqualTo("1");
        assertThat(request.getFromDate()).isEqualTo("2026-05-26 10:14:00");
        assertThat(entityCaptor.getValue().getHeaders().getFirst("access-token"))
                .isEqualTo("token-1");
    }

    @Test
    void getLivePricePostsLtpRequestAndReturnsLastPrice() {
        when(instrumentResolverService.resolve("RELIANCE"))
                .thenReturn(new InstrumentInfo("2885", "NSE_EQ", "EQUITY"));

        DhanLtpResponse.DhanLtpData ltpData =
                new DhanLtpResponse.DhanLtpData();
        ltpData.setLastPrice(2810.5);

        DhanLtpResponse response = new DhanLtpResponse();
        response.setStatus("success");
        response.setData(Map.of(
                "NSE_EQ",
                Map.of("2885", ltpData)
        ));

        when(restTemplate.postForEntity(
                eq("https://api.dhan.co/v2/marketfeed/ltp"),
                org.mockito.ArgumentMatchers.any(HttpEntity.class),
                eq(DhanLtpResponse.class)
        )).thenReturn(ResponseEntity.ok(response));

        Double livePrice =
                marketDataService.getLivePrice("RELIANCE");

        assertThat(livePrice).isEqualTo(2810.5);

        ArgumentCaptor<HttpEntity<Map<String, List<Integer>>>> entityCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("https://api.dhan.co/v2/marketfeed/ltp"),
                entityCaptor.capture(),
                eq(DhanLtpResponse.class)
        );

        assertThat(entityCaptor.getValue().getBody())
                .containsEntry("NSE_EQ", List.of(2885));
        assertThat(entityCaptor.getValue().getHeaders().getFirst("access-token"))
                .isEqualTo("token-1");
        assertThat(entityCaptor.getValue().getHeaders().getFirst("client-id"))
                .isEqualTo("client-1");
    }

    private Long epoch(LocalDateTime timestamp) {
        return timestamp
                .atZone(ZoneId.systemDefault())
                .toEpochSecond();
    }
}
