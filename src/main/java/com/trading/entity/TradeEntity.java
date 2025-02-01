package com.trading.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "trades")
public class TradeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "buy_order_id")
  private OrderEntity buyOrder;

  @ManyToOne
  @JoinColumn(name = "sell_order_id")
  private OrderEntity sellOrder;

  @ManyToOne
  @JoinColumn(name = "instrument_id")
  private InstrumentEntity instrument;

  private BigDecimal price;
  private int quantity;
  private Instant timestamp;
}
