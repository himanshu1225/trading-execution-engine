package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.market.InstrumentInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("dhan")
public class DhanInstrumentResolverService {

    private final RestTemplate restTemplate;

    @Value("${dhan.base-url}")
    private String dhanBaseUrl;

    @Value("${dhan.access-token}")
    private String accessToken;

    private final Map<String, InstrumentInfo> instrumentCache =
            new HashMap<>();

    @PostConstruct
    public void loadInstruments() {

        log.info("Loading Dhan instruments...");

        String url = dhanBaseUrl + "/instrument/NSE_EQ";

        RequestEntity<Void> request =
                RequestEntity
                        .get(URI.create(url))
                        .header("access-token", accessToken)
                        .build();

        ResponseEntity<String> response =
                restTemplate.exchange(request, String.class);

        parseCsv(response.getBody());

        log.info("Loaded {} instruments",
                instrumentCache.size());
    }

    private void parseCsv(String csvData) {

        String[] lines = csvData.split("\\r?\\n");

        for (int i = 1; i < lines.length; i++) {

            String[] cols = lines[i].split(",");

            if (cols.length < 8) {
                continue;
            }

            String securityId = cols[2].trim();
            String instrument = cols[4].trim();
            String symbolName = cols[6].trim();
//            if (symbolName.contains("RELI")) {
//                System.out.println("Loaded symbol = " + symbolName);
//            }

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
            throw new RuntimeException(
                    "Instrument not found for symbol: " + symbol
            );
        }

        return instrument;
    }
}