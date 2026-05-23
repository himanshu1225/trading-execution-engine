package com.tradingbot.trading_execution_engine.execution.service;

import com.tradingbot.trading_execution_engine.broker.model.BrokerProductType;
import com.tradingbot.trading_execution_engine.persistence.entity.Signal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Service
@Slf4j
public class ExecutionProductResolver {

    private static final LocalTime HIT_INTRADAY_CUTOFF =
            LocalTime.of(13, 30);

    private static final DateTimeFormatter ALERT_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public BrokerProductType resolveForOrder(Signal signal) {
        String tradeType =
                normalizeTradeType(signal.getTradeType());

        if (!"HIT".equals(tradeType)) {
            return BrokerProductType.CNC;
        }

        LocalTime alertTime =
                parseAlertTime(signal);

        if (alertTime == null) {
            return BrokerProductType.CNC;
        }

        if (alertTime.isBefore(HIT_INTRADAY_CUTOFF)) {
            return BrokerProductType.INTRADAY;
        }

        return BrokerProductType.CNC;
    }

    public BrokerProductType resolveForPersistentOrder(Signal signal) {
        return BrokerProductType.CNC;
    }

    private String normalizeTradeType(String tradeType) {
        if (tradeType == null) {
            return "";
        }

        return tradeType.trim()
                .toUpperCase(Locale.ROOT);
    }

    private LocalTime parseAlertTime(Signal signal) {
        try {
            return LocalDateTime
                    .parse(
                            signal.getAlertDateTimeStamp(),
                            ALERT_TIMESTAMP_FORMATTER
                    )
                    .toLocalTime();

        } catch (DateTimeParseException | NullPointerException e) {
            log.warn(
                    "Unable to parse alert timestamp for product type resolution, signalId={}, timestamp={}",
                    signal.getId(),
                    signal.getAlertDateTimeStamp()
            );
            return null;
        }
    }
}
