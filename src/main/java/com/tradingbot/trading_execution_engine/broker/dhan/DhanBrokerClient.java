package com.tradingbot.trading_execution_engine.broker.dhan;

import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanForeverOrderRequest;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanOrderStatusResponse;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanPlaceOrderRequest;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanPlaceOrderResponse;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanSuperOrderModifyRequest;
import com.tradingbot.trading_execution_engine.broker.dhan.dto.DhanSuperOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
@Profile("dhan")
@RequiredArgsConstructor
public class DhanBrokerClient {

    private final RestTemplate restTemplate;
    private final DhanBrokerProperties properties;

    public DhanPlaceOrderResponse placeOrder(DhanPlaceOrderRequest request) {
        RequestEntity<DhanPlaceOrderRequest> entity =
                RequestEntity
                        .post(uri("/orders"))
                        .headers(this::applyHeaders)
                        .body(request);

        ResponseEntity<DhanPlaceOrderResponse> response =
                restTemplate.exchange(entity, DhanPlaceOrderResponse.class);

        return response.getBody();
    }

    public DhanPlaceOrderResponse placeForeverOrder(
            DhanForeverOrderRequest request) {

        RequestEntity<DhanForeverOrderRequest> entity =
                RequestEntity
                        .post(uri("/forever/orders"))
                        .headers(this::applyHeaders)
                        .body(request);

        ResponseEntity<DhanPlaceOrderResponse> response =
                restTemplate.exchange(entity, DhanPlaceOrderResponse.class);

        return response.getBody();
    }

    public DhanOrderStatusResponse getOrderStatus(String orderId) {
        RequestEntity<Void> entity =
                RequestEntity
                        .get(uri("/orders/" + orderId))
                        .headers(this::applyHeaders)
                        .build();

        ResponseEntity<DhanOrderStatusResponse> response =
                restTemplate.exchange(entity, DhanOrderStatusResponse.class);

        return response.getBody();
    }

    public DhanPlaceOrderResponse placeSuperOrder(
            DhanSuperOrderRequest request) {

        RequestEntity<DhanSuperOrderRequest> entity =
                RequestEntity
                        .post(uri("/super/orders"))
                        .headers(this::applyHeaders)
                        .body(request);

        ResponseEntity<DhanPlaceOrderResponse> response =
                restTemplate.exchange(entity, DhanPlaceOrderResponse.class);

        return response.getBody();
    }

    public DhanPlaceOrderResponse modifySuperOrder(
            String orderId,
            DhanSuperOrderModifyRequest request) {

        RequestEntity<DhanSuperOrderModifyRequest> entity =
                RequestEntity
                        .put(uri("/super/orders/" + orderId))
                        .headers(this::applyHeaders)
                        .body(request);

        ResponseEntity<DhanPlaceOrderResponse> response =
                restTemplate.exchange(entity, DhanPlaceOrderResponse.class);

        return response.getBody();
    }

    public DhanPlaceOrderResponse cancelSuperOrderLeg(
            String orderId,
            String orderLeg) {

        RequestEntity<Void> entity =
                RequestEntity
                        .delete(uri("/super/orders/" + orderId + "/" + orderLeg))
                        .headers(this::applyHeaders)
                        .build();

        ResponseEntity<DhanPlaceOrderResponse> response =
                restTemplate.exchange(entity, DhanPlaceOrderResponse.class);

        return response.getBody();
    }

    public DhanPlaceOrderResponse cancelOrder(String orderId) {
        RequestEntity<Void> entity =
                RequestEntity
                        .delete(uri("/orders/" + orderId))
                        .headers(this::applyHeaders)
                        .build();

        ResponseEntity<DhanPlaceOrderResponse> response =
                restTemplate.exchange(entity, DhanPlaceOrderResponse.class);

        return response.getBody();
    }

    private URI uri(String path) {
        return URI.create(properties.getBaseUrl() + path);
    }

    private void applyHeaders(HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access-token", properties.getAccessToken());
    }
}
