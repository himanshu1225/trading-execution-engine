package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.integration.dto.upstox.UpstoxSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpstoxInstrumentResolverService {

    private final RestTemplate restTemplate;

    @Value("${upstox.base-url}")
    private String upstoxBaseUrl;

    @Value("${upstox.access-token}")
    private String accessToken;

    private final Map<String, String> instrumentCache =
            new ConcurrentHashMap<>();

    public String resolveInstrumentKey(String symbol) {

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        // CACHE HIT
        if (instrumentCache.containsKey(normalizedSymbol)) {
            log.info("CACHE HIT for {}", normalizedSymbol);
            return instrumentCache.get(normalizedSymbol);
        }

        // CACHE MISS
        log.info("CACHE MISS for {}", normalizedSymbol);

        String url =
                UriComponentsBuilder
                        .fromHttpUrl(upstoxBaseUrl + "/v2/instruments/search")
                        .queryParam("query", normalizedSymbol)
                        .queryParam("exchanges", "NSE")
                        .queryParam("segments", "EQ")
                        .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/json");

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);

        ResponseEntity<UpstoxSearchResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        UpstoxSearchResponse.class
                );

        UpstoxSearchResponse body = response.getBody();

        if (body == null ||
                body.getData() == null ||
                body.getData().isEmpty()) {
            throw new RuntimeException(
                    "No Upstox instrument found for symbol: " + symbol
            );
        }

        String instrumentKey =
                body.getData().get(0).getInstrument_key();

        instrumentCache.put(normalizedSymbol, instrumentKey);

        log.info("Cached {} -> {}",
                normalizedSymbol,
                instrumentKey);

        return instrumentKey;
    }
}