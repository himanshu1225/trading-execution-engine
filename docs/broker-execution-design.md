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
