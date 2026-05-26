package com.tradingbot.trading_execution_engine.broker.dhan.service;

import com.tradingbot.trading_execution_engine.broker.dhan.client.DhanBrokerClient;
import com.tradingbot.trading_execution_engine.broker.dhan.config.DhanBrokerProperties;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanMarginRequest;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanMarginResponse;
import com.tradingbot.trading_execution_engine.broker.model.MarginCheckRequest;
import com.tradingbot.trading_execution_engine.broker.model.MarginCheckResponse;
import com.tradingbot.trading_execution_engine.broker.service.BrokerMarginService;
import com.tradingbot.trading_execution_engine.marketdata.model.InstrumentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dhan")
@RequiredArgsConstructor
@Slf4j
public class DhanBrokerMarginService implements BrokerMarginService {

    private final DhanBrokerClient dhanBrokerClient;
    private final DhanBrokerProperties properties;
    private final DhanInstrumentResolverService instrumentResolverService;

    @Override
    public MarginCheckResponse checkMargin(MarginCheckRequest request) {
        InstrumentInfo instrument =
                instrumentResolverService.resolve(request.getSymbol());

        DhanMarginRequest dhanRequest =
                DhanMarginRequest.builder()
                        .dhanClientId(properties.getClientId())
                        .exchangeSegment(instrument.getExchangeSegment())
                        .transactionType(request.getSide().name())
                        .quantity(request.getQuantity())
                        .productType(request.getProductType().name())
                        .securityId(instrument.getSecurityId())
                        .price(request.getPrice())
                        .triggerPrice(triggerPriceOrZero(request))
                        .build();

        DhanMarginResponse dhanResponse =
                dhanBrokerClient.calculateMargin(dhanRequest);

        if (dhanResponse == null) {
            throw new IllegalStateException("Dhan margin response is empty");
        }

        log.info(
                "Dhan margin checked symbol={}, product={}, qty={}, totalMargin={}, availableBalance={}, insufficientBalance={}",
                request.getSymbol(),
                request.getProductType(),
                request.getQuantity(),
                dhanResponse.getTotalMargin(),
                dhanResponse.getAvailableBalance(),
                dhanResponse.getInsufficientBalance()
        );

        return MarginCheckResponse.builder()
                .totalMargin(dhanResponse.getTotalMargin())
                .availableBalance(dhanResponse.getAvailableBalance())
                .insufficientBalance(dhanResponse.getInsufficientBalance())
                .message("Dhan margin check completed")
                .build();
    }

    private Double triggerPriceOrZero(MarginCheckRequest request) {
        if (request.getTriggerPrice() == null) {
            return 0.0;
        }

        return request.getTriggerPrice();
    }
}
