package com.trading.dto;

import com.trading.entity.OrderDirection;
import com.trading.entity.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CompositeInstrumentRequest(
    @NotBlank String symbol,
    @Positive int quantity,
    OrderDirection direction,
    OrderType type,
    @Positive BigDecimal price) {}
