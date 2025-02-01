package com.trading.service;

import com.trading.dto.CompositeInstrumentRequest;
import com.trading.entity.*;
import com.trading.repository.CompositeOrderRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompositeOrderService {

  private final CompositeOrderRepository compositeOrderRepository;
  private final OrderService orderService;
  private final OrderBookService orderBookService;

  @Transactional
  public UUID createCompositeOrder(List<CompositeInstrumentRequest> instruments, String traderId) {
    CompositeOrder composite = new CompositeOrder();
    compositeOrderRepository.save(composite);

    for (CompositeInstrumentRequest instr : instruments) {
      OrderEntity order = new OrderEntity();
      order.setTraderId(traderId);
      order.setInstrument(orderBookService.getOrCreateInstrument(instr.symbol()));
      order.setDirection(instr.direction());
      order.setType(instr.type());
      order.setQuantity(instr.quantity());
      order.setPrice(instr.price());
      order.setCompositeOrder(composite);

      orderService.placeOrder(order);
    }
    return composite.getCompositeId();
  }

  public CompositeOrderStatus getCompositeStatus(UUID compositeId) {
    CompositeOrder comp =
        compositeOrderRepository
            .findById(compositeId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid composite order"));

    long filled =
        comp.getUnderlyingOrders().stream()
            .filter(o -> o.getStatus() == OrderStatus.FILLED)
            .count();

    if (filled == 0) {
      return CompositeOrderStatus.PENDING;
    } else if (filled == comp.getUnderlyingOrders().size()) {
      return CompositeOrderStatus.FILLED;
    } else {
      return CompositeOrderStatus.PARTIALLY_FILLED;
    }
  }
}
