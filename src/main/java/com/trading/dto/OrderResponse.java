package com.trading.dto;

import com.trading.entity.OrderDirection;
import com.trading.entity.OrderStatus;
import com.trading.entity.OrderType;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
    String orderId,
    String traderId,
    String instrumentSymbol,
    OrderDirection direction,
    OrderType type,
    Integer quantity,
    BigDecimal price,
    OrderStatus status,
    Instant createdAt) {}
