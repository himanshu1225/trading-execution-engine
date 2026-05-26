# Manual Dev Test Runbook

This runbook is for controlled dev testing of the TradingView alert to Dhan execution flow.

## Start App

Use the Dhan profile for real broker execution:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dhan
```

Use the default profile for mock broker execution.

## TradingView Alert Test

Send a webhook alert with:

- valid `symbol`
- `tradeType`
- `tradeScore >= 5`
- `entryPrice`
- `stopLossPrice`
- alert timestamp

Expected result:

- signal is saved
- trade is rejected if `tradeScore < 5`
- accepted trade gets target plan:
  - `oneRPrice`
  - `onePointFiveRPrice`
  - `twoRPrice`
  - `targetPrice = twoRPrice`
  - `trailingJump`

## Database Checks

Check `signals`:

- `status` should reflect decision/execution state
- `target_price` should be filled
- `onerprice`, `one_point_fiverprice`, `tworprice` should be filled
- `trailing_jump` should be filled

Check `orders`:

- `order_type = SUPER_ORDER`
- `product_type` is resolved from trade type/time
- `security_id` is filled
- `exchange_segment` is filled
- `stop_loss_price` is filled
- `target_price` is filled
- `trailing_jump` is filled

Check `order_legs`:

- one `ENTRY_LEG`
- one `TARGET_LEG`
- one `STOP_LOSS_LEG`
- initial `leg_status = PENDING`

## Manual Dhan Postback Test

Endpoint:

```http
POST /dhan/postback/order-update
```

Use a real `orders.broker_order_id` when testing against a saved order.

Entry traded example:

```json
{
  "orderId": "REAL_OR_LOCAL_BROKER_ORDER_ID",
  "orderStatus": "TRADED",
  "orderLegName": "ENTRY_LEG",
  "price": 100.0,
  "quantity": 1
}
```

Expected result:

- `ENTRY_LEG` becomes `TRADED`
- parent order becomes `PLACED`

Target traded example:

```json
{
  "orderId": "REAL_OR_LOCAL_BROKER_ORDER_ID",
  "orderStatus": "TRADED",
  "orderLegName": "TARGET_LEG",
  "price": 120.0,
  "quantity": 1
}
```

Expected result:

- `TARGET_LEG` becomes `TRADED`
- parent order becomes `FILLED`
- `filled_at` is set

If a postback arrives for an existing order but a missing leg row, the app creates the missing leg row from the postback.

## Manual Reconciliation Test

Endpoint:

```http
POST /orders/reconcile
```

Purpose:

Detect when a Super Order position was manually squared off in Dhan and pending target/SL legs still need cancellation.

The app checks:

- local active `SUPER_ORDER`s
- entry leg is already `TRADED`
- target/SL legs are still `PENDING`
- Dhan `/positions` using `securityId + exchangeSegment + productType`

If Dhan still has an open position, expected response:

```json
{
  "checkedOrders": 1,
  "skippedOrders": 0,
  "closedPositionsDetected": 0,
  "cancelledExitLegs": 0
}
```

If Dhan shows closed position or `netQty = 0`, expected response:

```json
{
  "checkedOrders": 1,
  "skippedOrders": 0,
  "closedPositionsDetected": 1,
  "cancelledExitLegs": 2
}
```

After successful reconciliation:

- `TARGET_LEG = CANCELLED`
- `STOP_LOSS_LEG = CANCELLED`
- `cancelled_at` is set

## Live Dhan Safety Notes

- Test with 1 share only.
- Prefer a liquid stock.
- Use CNC Super Order for carry-forward behavior.
- Keep reconciliation manual until one live test is verified.
- Do not enable scheduler until live behavior is confirmed.
- Remember that broker API calls cannot be rolled back by DB transactions.

## Current Broker Split

- Market data for decisions: Upstox
- Broker execution: Mock by default, Dhan with `dhan` profile
- Dhan market data service is inactive unless `dhan-marketdata` profile is enabled
