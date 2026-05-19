package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.integration.dto.DhanIntradayRequest;
import com.tradingbot.trading_execution_engine.integration.dto.DhanIntradayResponse;
import com.tradingbot.trading_execution_engine.market.Candle;
import com.tradingbot.trading_execution_engine.market.InstrumentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Profile("dhan")
public class DhanMarketDataService implements MarketDataService{

    private final RestTemplate restTemplate;
    private final DhanInstrumentResolverService instrumentResolverService;

    @Value("${dhan.base-url}")
    private String dhanBaseUrl;

    @Value("${dhan.access-token}")
    private String accessToken;

    private static final DateTimeFormatter DHAN_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
                        alertTimestamp.format(DHAN_FORMAT),
                        LocalDateTime.now().format(DHAN_FORMAT)
                );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access-token", accessToken);

        HttpEntity<DhanIntradayRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<DhanIntradayResponse> response =
                restTemplate.postForEntity(
                        dhanBaseUrl + "/charts/intraday",
                        entity,
                        DhanIntradayResponse.class
                );

        List<Candle> candles = mapResponse(response.getBody());

        System.out.println("Fetched candles: " + candles.size());

        if (!candles.isEmpty()) {
            System.out.println("First candle: " + candles.get(0).getTimestamp());
            System.out.println("Last candle: " + candles.get(candles.size() - 1).getTimestamp());
        }

        return candles;
    }

    @Override
    public Double getLivePrice(String symbol) {
        return 0.0;
    }

    private List<Candle> mapResponse(
            DhanIntradayResponse response) {

        List<Candle> candles = new ArrayList<>();

        for (int i = 0; i < response.getOpen().size(); i++) {

            LocalDateTime timestamp =
                    Instant.ofEpochSecond(
                                    response.getTimestamp().get(i)
                            )
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

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


        return candles;
    }
}