package com.tradingbot.trading_execution_engine.broker.service;

import com.tradingbot.trading_execution_engine.broker.model.MarginCheckRequest;
import com.tradingbot.trading_execution_engine.broker.model.MarginCheckResponse;

public interface BrokerMarginService {

    MarginCheckResponse checkMargin(MarginCheckRequest request);
}
