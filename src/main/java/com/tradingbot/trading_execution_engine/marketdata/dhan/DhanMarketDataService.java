package com.tradingbot.trading_execution_engine.marketdata.dhan;

import com.tradingbot.trading_execution_engine.broker.dhan.service.DhanInstrumentResolverService;
import com.tradingbot.trading_execution_engine.marketdata.dhan.dto.DhanIntradayRequest;
import com.tradingbot.trading_execution_engine.marketdata.dhan.dto.DhanIntradayResponse;
import com.tradingbot.trading_execution_engine.marketdata.dhan.dto.DhanLtpResponse;
import com.tradingbot.trading_execution_engine.marketdata.model.Candle;
import com.tradingbot.trading_execution_engine.marketdata.model.InstrumentInfo;
import com.tradingbot.trading_execution_engine.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Primary
@Profile("dhan-marketdata")
@RequiredArgsConstructor
@Slf4j
public class DhanMarketDataService implements MarketDataService {

    private final RestTemplate restTemplate;
    private final DhanInstrumentResolverService instrumentResolverService;

    @Value("${dhan.base-url}")
    private String dhanBaseUrl;

    @Value("${dhan.access-token}")
    private String accessToken;

    @Value("${dhan.client-id}")
    private String clientId;

    private static final DateTimeFormatter DHAN_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<Candle> getCandlesAfterAlert(
            String symbol,
            LocalDateTime alertTimestamp) {

        InstrumentInfo instrument =
                instrumentResolverService.resolve(symbol);

        DhanIntradayRequest request =
                new DhanIntradayRequest(
                        instrument.getSecurityId(),
                        instrument.getExchangeSegment(),
                        instrument.getInstrument(),
                        "1",
                        false,
                        alertTimestamp.minusMinutes(1).format(DHAN_FORMAT),
                        LocalDateTime.now().format(DHAN_FORMAT)
                );

        HttpEntity<DhanIntradayRequest> entity =
                new HttpEntity<>(request, jsonHeaders(false));

        ResponseEntity<DhanIntradayResponse> response =
                restTemplate.postForEntity(
                        dhanBaseUrl + "/charts/intraday",
                        entity,
                        DhanIntradayResponse.class
                );

        List<Candle> candles =
                mapResponse(response.getBody(), alertTimestamp);

        log.info(
                "Fetched Dhan candles symbol={}, securityId={}, candles={}",
                symbol,
                instrument.getSecurityId(),
                candles.size()
        );

        return candles;
    }

    @Override
    public Double getLivePrice(String symbol) {
        InstrumentInfo instrument =
                instrumentResolverService.resolve(symbol);

        Map<String, List<Integer>> request =
                Map.of(
                        instrument.getExchangeSegment(),
                        List.of(Integer.parseInt(instrument.getSecurityId()))
                );

        HttpEntity<Map<String, List<Integer>>> entity =
                new HttpEntity<>(request, jsonHeaders(true));

        ResponseEntity<DhanLtpResponse> response =
                restTemplate.postForEntity(
                        dhanBaseUrl + "/marketfeed/ltp",
                        entity,
                        DhanLtpResponse.class
                );

        Double livePrice =
                extractLivePrice(
                        response.getBody(),
                        instrument
                );

        log.info(
                "DHAN LIVE PRICE symbol={} securityId={} price={}",
                symbol,
                instrument.getSecurityId(),
                livePrice
        );

        return livePrice;
    }

    private HttpHeaders jsonHeaders(boolean includeClientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access-token", accessToken);

        if (includeClientId) {
            headers.set("client-id", clientId);
        }

        return headers;
    }

    private List<Candle> mapResponse(
            DhanIntradayResponse response,
            LocalDateTime alertTimestamp) {

        List<Candle> candles = new ArrayList<>();

        if (response == null ||
                response.getOpen() == null ||
                response.getTimestamp() == null) {

            log.warn("Empty response from Dhan intraday API");
            return candles;
        }

        LocalDateTime cutoff =
                alertTimestamp.minusMinutes(1);

        int candleCount =
                List.of(
                                response.getOpen().size(),
                                response.getHigh().size(),
                                response.getLow().size(),
                                response.getClose().size(),
                                response.getTimestamp().size()
                        )
                        .stream()
                        .min(Integer::compareTo)
                        .orElse(0);

        for (int i = 0; i < candleCount; i++) {

            LocalDateTime timestamp =
                    Instant.ofEpochSecond(
                                    response.getTimestamp().get(i)
                            )
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

            if (timestamp.isBefore(cutoff)) {
                continue;
            }

            candles.add(
                    new Candle(
                            timestamp,
                            response.getOpen().get(i),
                            response.getHigh().get(i),
                            response.getLow().get(i),
                            response.getClose().get(i)
                    )
            );
        }

        candles.sort(
                Comparator.comparing(Candle::getTimestamp)
        );

        return candles;
    }

    private Double extractLivePrice(
            DhanLtpResponse response,
            InstrumentInfo instrument) {

        if (response == null || response.getData() == null) {
            throw new RuntimeException("No LTP data returned from Dhan");
        }

        Map<String, DhanLtpResponse.DhanLtpData> segmentData =
                response.getData().get(instrument.getExchangeSegment());

        if (segmentData == null) {
            throw new RuntimeException(
                    "No Dhan LTP segment data returned for " +
                            instrument.getExchangeSegment()
            );
        }

        DhanLtpResponse.DhanLtpData ltpData =
                segmentData.get(instrument.getSecurityId());

        if (ltpData == null || ltpData.getLastPrice() == null) {
            throw new RuntimeException(
                    "No Dhan LTP data returned for securityId " +
                            instrument.getSecurityId()
            );
        }

        return ltpData.getLastPrice();
    }
}
