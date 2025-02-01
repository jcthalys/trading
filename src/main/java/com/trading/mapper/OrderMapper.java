package com.trading.mapper;

import com.trading.dto.OrderRequest;
import com.trading.dto.OrderResponse;
import com.trading.entity.InstrumentEntity;
import com.trading.entity.OrderEntity;
import java.time.Instant;

public class OrderMapper {

  public static OrderEntity toEntity(OrderRequest request) {
    return OrderEntity.builder()
        .traderId(request.traderId())
        .instrument(InstrumentEntity.builder().symbol(request.instrumentSymbol()).build())
        .direction(request.direction())
        .type(request.type())
        .quantity(request.quantity())
        .price(request.price())
        .createdAt(Instant.now())
        .build();
  }

  public static OrderResponse toResponse(OrderEntity entity) {
    return new OrderResponse(
        entity.getId().toString(),
        entity.getTraderId(),
        entity.getInstrument().getSymbol(),
        entity.getDirection(),
        entity.getType(),
        switch (entity.getStatus()) {
          case OPEN, FILLED, CANCELLED -> entity.getQuantity();
          case PARTIALLY_FILLED -> entity.getRemainingQuantity();
        },
        entity.getPrice(),
        entity.getStatus(),
        entity.getCreatedAt());
  }
}
