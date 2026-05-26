# Broker Execution Design

This document captures the current broker execution behavior and Dhan-specific safety checks.

## Responsibility Split

- `marketdata/*`: price and candle data used for decision making.
- `broker/*`: broker-neutral order placement, margin checks, and execution integration.
- `execution/*`: converts valid trade decisions into broker requests.
- `order/*`: order lifecycle, monitoring, leg cancellation, and reconciliation.
- `persistence/*`: JPA entities and repositories.

Current provider split:

- Market data: Upstox
- Broker execution: Mock by default, Dhan when the `dhan` profile is active

Optional Dhan market-data mode:

```text
dhan-marketdata profile
 -> MarketDataService uses Dhan
 -> UpstoxMarketDataService is disabled
```

Run examples:

```bash
# Dhan broker execution, Upstox market data
mvn spring-boot:run -Dspring-boot.run.profiles=dhan

# Dhan broker execution, Dhan market data
mvn spring-boot:run -Dspring-boot.run.profiles=dhan,dhan-marketdata

# Mock broker execution, Dhan market data
mvn spring-boot:run -Dspring-boot.run.profiles=dhan-marketdata
```

## Dhan Package Layout

```text
broker/dhan
  client        Dhan HTTP API client
  config        Dhan properties
  controller    Dhan webhook/postback endpoints
  dto           Dhan request/response DTOs
  service       Dhan broker, margin, postback, reconciliation services
```

## Execution Flow

```text
TradingView alert
 -> SignalService
 -> TradeDecisionEngine
 -> RiskManagementService
 -> Signal saved
 -> ExecutionService
 -> BrokerMarginService.checkMargin()
 -> BrokerOrderService.placeSuperOrder()
 -> Order and OrderLeg rows saved
```

## Margin Pre-Check

Dhan margin check uses:

```http
POST /margincalculator
```

The app calls this before placing a Dhan Super Order.

Policy:

```text
insufficientBalance == 0
 -> place order

insufficientBalance > 0
 -> reject signal before placing order
 -> do not create order/order_legs

margin API fails
 -> log warning
 -> continue placing order
```

Reasoning:

- A positive `insufficientBalance` is a clear Dhan response that funds are short.
- A margin API failure is unknown state, not proof that funds are unavailable.
- Dhan order placement remains the final authority and will reject if funds are actually insufficient.

## Margin Abstraction

Broker-neutral API:

```text
BrokerMarginService
MarginCheckRequest
MarginCheckResponse
```

Implementations:

- `MockBrokerMarginService`: always returns sufficient funds.
- `DhanBrokerMarginService`: resolves Dhan instrument details and calls Dhan margin calculator.

## Market Data Providers

Market-data providers implement:

```text
MarketDataService.getCandlesAfterAlert(symbol, alertTimestamp)
MarketDataService.getLivePrice(symbol)
```

Default provider:

```text
UpstoxMarketDataService
```

Dhan provider:

```text
DhanMarketDataService
```

Dhan market data uses:

```http
POST /charts/intraday
POST /marketfeed/ltp
```

Dhan candle behavior matches the Upstox flow:

- request 1-minute candles from one minute before alert timestamp
- filter candles before alert minus one minute
- sort candles by timestamp
- return `Candle` objects for price-path analysis

Dhan live price behavior:

- resolve `symbol -> securityId + exchangeSegment`
- call `/marketfeed/ltp`
- return `last_price`

Dhan market-data requires:

- `dhan.base-url`
- `dhan.access-token`
- `dhan.client-id`
- Dhan instrument resolver active through `dhan-marketdata`

## Super Order Lifecycle

For valid trades, the app places Dhan Super Orders with:

- entry leg
- target leg
- stop-loss leg
- trailing jump

Local leg tracking:

```text
ENTRY_LEG
TARGET_LEG
STOP_LOSS_LEG
```

Each leg is stored in `order_legs`.

## Manual Square-Off Reconciliation

Dhan confirmed that for CNC Super Orders, target and stop-loss legs can remain valid after manual square-off. The app provides manual reconciliation:

```http
POST /orders/reconcile
```

The reconciliation checks Dhan positions using:

```text
securityId + exchangeSegment + productType
```

If the position is closed but local target/SL legs are still pending, the app cancels pending exit legs.

## Scheduler Policy

Reconciliation is currently manual.

Do not add scheduler until one live Dhan test confirms:

- `/positions` matching works for real orders
- manual square-off leaves pending exit legs
- Dhan accepts target/SL leg cancellation through API
