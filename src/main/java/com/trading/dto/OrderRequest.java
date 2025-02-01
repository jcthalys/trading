package com.trading.dto;

import com.trading.entity.OrderDirection;
import com.trading.entity.OrderType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderRequest(
    @NotBlank(message = "Trader ID is required") String traderId,
    @NotBlank(message = "Instrument symbol is required") String instrumentSymbol,
    @NotNull(message = "Direction of order is required (BUY or SELL)") OrderDirection direction,
    @NotNull(message = "Order type is required (MARKET or LIMIT)") OrderType type,
    @Min(value = 1, message = "Minimum quantity is 1") Integer quantity,
    @Positive(message = "Price must be positive when specified") BigDecimal price) {

  public OrderRequest {
    if (type == OrderType.LIMIT && price == null) {
      throw new IllegalArgumentException("Limit orders require a price");
    }
    if (type == OrderType.MARKET && price != null) {
      throw new IllegalArgumentException("Market orders cannot have a price");
    }
  }
}
