package com.tradingbot.trading_execution_engine.broker.dhan;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Component
@Profile("dhan")
public class DhanBrokerProperties {

    @Value("${dhan.base-url}")
    private String baseUrl;

    @Value("${dhan.access-token}")
    private String accessToken;

    @Value("${dhan.client-id}")
    private String clientId;

    @Value("${dhan.order.product-type:INTRADAY}")
    private String orderProductType;

    @Value("${dhan.order.validity:DAY}")
    private String orderValidity;

    @Value("${dhan.forever.product-type:CNC}")
    private String foreverProductType;

    @Value("${dhan.forever.validity:DAY}")
    private String foreverValidity;
}
