package com.tradingbot.trading_execution_engine.marketdata.upstox;

import com.tradingbot.trading_execution_engine.marketdata.upstox.dto.UpstoxHistoricalResponse;
import com.tradingbot.trading_execution_engine.marketdata.upstox.dto.UpstoxLtpData;
import com.tradingbot.trading_execution_engine.marketdata.upstox.dto.UpstoxLtpResponse;
import com.tradingbot.trading_execution_engine.marketdata.model.Candle;
import com.tradingbot.trading_execution_engine.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class UpstoxMarketDataService implements MarketDataService {

    private final RestTemplate restTemplate;
    private final UpstoxInstrumentResolverService resolverService;

    @Value("${upstox.base-url}")
    private String upstoxBaseUrl;

    @Value("${upstox.access-token}")
    private String accessToken;

    @Override
    public List<Candle> getCandlesAfterAlert(
            String symbol,
            LocalDateTime alertTimestamp) {

        String instrumentKey =
                resolverService.resolveInstrumentKey(symbol);

        LocalDate alertDate = alertTimestamp.toLocalDate();
        LocalDate today = LocalDate.now();

        String url;

        // Current trading day -> intraday API
        if (alertDate.equals(today)) {

            url =
                    upstoxBaseUrl +
                            "/v3/historical-candle/intraday/" +
                            instrumentKey +
                            "/minutes/1";

            log.info(
                    "Using Upstox INTRADAY API for symbol={}, alertDate={}",
                    symbol,
                    alertDate
            );

        } else {

            // Historical day -> historical API
            url =
                    upstoxBaseUrl +
                            "/v3/historical-candle/" +
                            instrumentKey +
                            "/minutes/1/" +
                            alertDate +
                            "/" +
                            alertDate;

            log.info(
                    "Using Upstox HISTORICAL API for symbol={}, alertDate={}",
                    symbol,
                    alertDate
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);

        ResponseEntity<UpstoxHistoricalResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        UpstoxHistoricalResponse.class
                );

        log.info(
                "Resolved instrument key={}, URL={}",
                instrumentKey,
                url
        );

        return mapResponse(
                response.getBody(),
                alertTimestamp
        );
    }

    private List<Candle> mapResponse(
            UpstoxHistoricalResponse response,
            LocalDateTime alertTimestamp) {

        List<Candle> candles = new ArrayList<>();

        if (response == null ||
                response.getData() == null ||
                response.getData().getCandles() == null) {

            log.warn("Empty response from Upstox historical API");
            return candles;
        }

        LocalDateTime cutoff =
                alertTimestamp.minusMinutes(1);

        for (List<Object> raw : response.getData().getCandles()) {

            LocalDateTime candleTime =
                    OffsetDateTime
                            .parse((String) raw.get(0))
                            .toLocalDateTime();

            if (candleTime.isBefore(cutoff)) {
                continue;
            }

            candles.add(
                    new Candle(
                            candleTime,
                            ((Number) raw.get(1)).doubleValue(),
                            ((Number) raw.get(2)).doubleValue(),
                            ((Number) raw.get(3)).doubleValue(),
                            ((Number) raw.get(4)).doubleValue()
                    )
            );
        }

        candles.sort(
                Comparator.comparing(Candle::getTimestamp)
        );

        log.info("Fetched candles={}", candles.size());

        return candles;
    }

    @Override
    public Double getLivePrice(String symbol) {

        String instrumentKey =
                resolverService.resolveInstrumentKey(symbol);

        String url =
                upstoxBaseUrl +
                        "/v3/market-quote/ltp?instrument_key=" +
                        instrumentKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);

        ResponseEntity<UpstoxLtpResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        UpstoxLtpResponse.class
                );

        if (response.getBody() == null ||
                response.getBody().getData() == null ||
                response.getBody().getData().isEmpty()) {

            throw new RuntimeException(
                    "No LTP data returned from Upstox"
            );
        }

        UpstoxLtpData ltpData =
                response.getBody()
                        .getData()
                        .values()
                        .iterator()
                        .next();

        Double livePrice = ltpData.getLastPrice();

        log.info(
                "LIVE PRICE symbol={} instrumentKey={} price={}",
                symbol,
                instrumentKey,
                livePrice
        );

        return livePrice;
    }
}
