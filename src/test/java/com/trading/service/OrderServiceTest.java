package com.trading.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.trading.dto.OrderRequest;
import com.trading.dto.OrderResponse;
import com.trading.entity.*;
import com.trading.exception.OrderNotFoundException;
import com.trading.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private OrderBookService orderBookService;

  @InjectMocks private OrderService orderService;

  @Test
  void placeOrder_ShouldSaveAndReturnOrder() {
    OrderRequest request =
        new OrderRequest(
            UUID.randomUUID().toString(),
            "AAPL",
            OrderDirection.BUY,
            OrderType.LIMIT,
            100,
            new BigDecimal("150.0"));
    OrderEntity savedEntity = getOrderEntity();
    when(orderBookService.processOrder(any())).thenReturn(savedEntity);

    OrderResponse response = orderService.placeOrder(request);

    assertNotNull(response.orderId());
  }

  @Test
  void getOrder_ShouldReturnOrderWhenExists() {
    UUID orderId = UUID.randomUUID();
    OrderEntity entity = getOrderEntity();
    entity.setId(orderId);
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(entity));

    OrderResponse response = orderService.getOrder(orderId);

    assertEquals(orderId.toString(), response.orderId());
  }

  @Test
  void getOrder_ShouldThrowWhenNotFound() {
    UUID nonExistentOrderId = UUID.randomUUID();
    when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

    assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(nonExistentOrderId));
  }

  private static OrderEntity getOrderEntity() {
    InstrumentEntity instrumentEntity = new InstrumentEntity();
    instrumentEntity.setSymbol("AAPL");

    OrderEntity orderEntity = new OrderEntity();
    orderEntity.setId(UUID.randomUUID());
    orderEntity.setTraderId("TRADER1");
    orderEntity.setInstrument(instrumentEntity);
    orderEntity.setDirection(OrderDirection.BUY);
    orderEntity.setType(OrderType.LIMIT);
    orderEntity.setQuantity(100);
    orderEntity.setPrice(new BigDecimal("150.00"));
    orderEntity.setStatus(OrderStatus.OPEN);
    orderEntity.setRemainingQuantity(100);
    orderEntity.setCompositeOrder(null);

    Instant now = Instant.now();
    orderEntity.setCreatedAt(now);
    return orderEntity;
  }
}
