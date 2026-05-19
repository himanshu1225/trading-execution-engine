# Trading Execution Engine

## Overview

Automated trade execution engine for TradingView-based stock trading strategies.

The system receives TradingView webhook alerts, validates market structure using historical price action, checks live market price, and makes rule-based order execution decisions.

---

## Current Entry Decision Workflow

### Market Data Validation
The engine validates setup quality using:

- Historical candle data
- Same-day intraday candle data
- Live LTP (Last Traded Price)

Smart API routing:

- Same-day alerts → Intraday API
- Previous-day alerts → Historical API
- Live execution pricing → LTP API

---

## Supported Trade Decision Scenarios

### 1. FRESH_SETUP
Condition:
- Entry price never touched after alert
- Stop loss not broken

Action:
- Place LIMIT order at entry price

Example:
1380 → 1370 → 1360 → 1350

---

### 2. BETTER_ENTRY
Condition:
- Price moved below entry
- Stop loss remains safe
- Better live price available

Action:
- Execute MARKET order at live LTP

Example:
1380 → 1350 → 1340 → 1338

---

### 3. STOPLOSS_BROKEN
Condition:
- Stop loss breached after alert

Action:
- Reject trade setup

Example:
1380 → 1340 → 1328

---

### 4. THRESHOLD_BOUNCE
Condition:
- Entry touched
- Stop loss safe
- Bounce from entry remains within threshold

Action:
- Place LIMIT order at entry

Example:
1380 → 1340 → 1338 → 1346

---

### 5. STALE_MOVE
Condition:
- Entry touched
- Stop loss safe
- Bounce exceeds threshold

Action:
- Reject setup

Example:
1380 → 1340 → 1338 → 1360

---

## Persistence

The system persists:

### Signals
- Alert metadata
- Decision reason
- Execution status
- Actual entry price

### Orders
- MARKET orders
- LIMIT orders
- Pending states
- Broker execution tracking

---

## Validated Scenarios

Successfully tested end-to-end:

- BETTER_ENTRY
- FRESH_SETUP
- STOPLOSS_BROKEN
- THRESHOLD_BOUNCE
- STALE_MOVE

Includes:

- Decision engine validation
- DB persistence validation
- Order creation validation

---

## Next Planned Features

- Stop loss order placement
- Target order logic
- OCO / bracket handling
- Pending order stale cancellation
- Broker order status sync
- Exit management engine
