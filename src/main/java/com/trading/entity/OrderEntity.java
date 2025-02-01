package com.trading.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "orders")
public class OrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String traderId;

  @Enumerated(EnumType.STRING)
  private OrderDirection direction;

  @Enumerated(EnumType.STRING)
  private OrderType type;

  private BigDecimal price;
  private int quantity;
  private int remainingQuantity;
  private Instant createdAt;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "instrument_id")
  private InstrumentEntity instrument;

  @Enumerated(EnumType.STRING)
  private OrderStatus status;

  @ManyToOne
  @JoinColumn(name = "composite_id")
  private CompositeOrder compositeOrder;
}
