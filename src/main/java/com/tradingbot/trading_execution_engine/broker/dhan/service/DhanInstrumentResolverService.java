package com.tradingbot.trading_execution_engine.broker.dhan.service;

import com.tradingbot.trading_execution_engine.broker.dhan.config.DhanBrokerProperties;
import com.tradingbot.trading_execution_engine.marketdata.model.InstrumentInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
@Profile({"dhan", "dhan-marketdata"})
@RequiredArgsConstructor
@Slf4j
public class DhanInstrumentResolverService {

    private final RestTemplate restTemplate;
    private final DhanBrokerProperties properties;

    private final Map<String, InstrumentInfo> instrumentCache =
            new HashMap<>();

    @PostConstruct
    public void loadInstruments() {

        log.info("Loading Dhan instruments for broker execution...");

        String url = properties.getBaseUrl() + "/instrument/NSE_EQ";

        RequestEntity<Void> request =
                RequestEntity
                        .get(URI.create(url))
                        .header("access-token", properties.getAccessToken())
                        .build();

        ResponseEntity<String> response =
                restTemplate.exchange(request, String.class);

        parseCsv(response.getBody());

        log.info("Loaded {} Dhan instruments", instrumentCache.size());
    }

    private void parseCsv(String csvData) {
        if (csvData == null || csvData.isBlank()) {
            return;
        }

        String[] lines = csvData.split("\\r?\\n");

        for (int i = 1; i < lines.length; i++) {
            String[] cols = lines[i].split(",");

            if (cols.length < 8) {
                continue;
            }

            String securityId = cols[2].trim();
            String instrument = cols[4].trim();
            String symbolName = cols[6].trim();

            instrumentCache.put(
                    symbolName,
                    new InstrumentInfo(
                            securityId,
                            "NSE_EQ",
                            instrument
                    )
            );
        }
    }

    public InstrumentInfo resolve(String symbol) {

        InstrumentInfo instrument =
                instrumentCache.get(symbol);

        if (instrument == null) {
            throw new IllegalArgumentException(
                    "Dhan instrument not found for symbol: " + symbol
            );
        }

        return instrument;
    }
}
