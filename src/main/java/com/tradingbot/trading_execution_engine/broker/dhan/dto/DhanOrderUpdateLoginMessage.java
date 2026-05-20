package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DhanOrderUpdateLoginMessage {

    @JsonProperty("LoginReq")
    private LoginRequest loginRequest;

    @JsonProperty("UserType")
    private String userType;

    @Getter
    @Builder
    public static class LoginRequest {

        @JsonProperty("MsgCode")
        private Integer messageCode;

        @JsonProperty("ClientId")
        private String clientId;

        @JsonProperty("Token")
        private String token;
    }
}
