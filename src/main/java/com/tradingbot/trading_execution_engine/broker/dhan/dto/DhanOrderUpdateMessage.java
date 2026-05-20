package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DhanOrderUpdateMessage {

    @JsonProperty("Data")
    private Data data;

    @JsonProperty("Type")
    private String type;

    @Getter
    @Setter
    public static class Data {

        @JsonProperty("Exchange")
        private String exchange;

        @JsonProperty("Segment")
        private String segment;

        @JsonProperty("Source")
        private String source;

        @JsonProperty("SecurityId")
        private String securityId;

        @JsonProperty("ClientId")
        private String clientId;

        @JsonProperty("ExchOrderNo")
        private String exchangeOrderNumber;

        @JsonProperty("OrderNo")
        private String orderNumber;

        @JsonProperty("Product")
        private String product;

        @JsonProperty("TxnType")
        private String transactionType;

        @JsonProperty("OrderType")
        private String orderType;

        @JsonProperty("Validity")
        private String validity;

        @JsonProperty("RemainingQuantity")
        private Integer remainingQuantity;

        @JsonProperty("Quantity")
        private Integer quantity;

        @JsonProperty("TradedQty")
        private Integer tradedQuantity;

        @JsonProperty("Price")
        private Double price;

        @JsonProperty("TriggerPrice")
        private Double triggerPrice;

        @JsonProperty("TradedPrice")
        private Double tradedPrice;

        @JsonProperty("AvgTradedPrice")
        private Double averageTradedPrice;

        @JsonProperty("OrderDateTime")
        private String orderDateTime;

        @JsonProperty("ExchOrderTime")
        private String exchangeOrderTime;

        @JsonProperty("LastUpdatedTime")
        private String lastUpdatedTime;

        @JsonProperty("ReasonDescription")
        private String reasonDescription;

        @JsonProperty("LegNo")
        private Integer legNumber;

        @JsonProperty("Symbol")
        private String symbol;

        @JsonProperty("ProductName")
        private String productName;

        @JsonProperty("Status")
        private String status;

        @JsonProperty("CorrelationId")
        private String correlationId;

        @JsonProperty("Remarks")
        private String remarks;
    }
}
