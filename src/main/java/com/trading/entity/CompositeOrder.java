package com.trading.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CompositeOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID compositeId;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "compositeOrder")
  private List<OrderEntity> underlyingOrders = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  private CompositeOrderStatus status = CompositeOrderStatus.PENDING;
}
