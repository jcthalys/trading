package com.trading.repository;

import com.trading.entity.*;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
  List<OrderEntity> findByInstrumentAndDirectionAndStatusInOrderByPriceDescCreatedAtAsc(
      InstrumentEntity instrument, OrderDirection direction, List<OrderStatus> statuses);

  List<OrderEntity> findByInstrumentAndDirectionAndStatusInOrderByPriceAscCreatedAtAsc(
      InstrumentEntity instrument, OrderDirection direction, List<OrderStatus> statuses);
}
