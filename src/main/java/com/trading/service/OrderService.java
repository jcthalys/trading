package com.trading.service;

import static com.trading.mapper.OrderMapper.toResponse;

import com.trading.dto.OrderRequest;
import com.trading.dto.OrderResponse;
import com.trading.entity.OrderEntity;
import com.trading.entity.OrderStatus;
import com.trading.exception.OrderNotFoundException;
import com.trading.mapper.OrderMapper;
import com.trading.repository.OrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderBookService orderBookService;

  public OrderResponse placeOrder(OrderRequest orderRequest) {
    OrderEntity order = OrderMapper.toEntity(orderRequest);
    if (order.getCompositeOrder() != null) {
      throw new IllegalArgumentException(
          "Composite orders must be placed via CompositeOrderService");
    }
    return toResponse(orderBookService.processOrder(order));
  }

  public OrderEntity placeOrder(OrderEntity order) {
    return orderBookService.processOrder(order);
  }

  @Transactional
  public void cancelOrder(UUID orderId) {
    OrderEntity order =
        orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    if (order.getStatus() != OrderStatus.OPEN) {
      throw new IllegalStateException("Cannot cancel a non-open order");
    }
    order.setStatus(OrderStatus.CANCELLED);
    orderRepository.save(order);
  }

  @Transactional(readOnly = true)
  public OrderResponse getOrder(UUID orderId) {
    return orderRepository
        .findById(orderId)
        .map(OrderMapper::toResponse)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
  }
}
