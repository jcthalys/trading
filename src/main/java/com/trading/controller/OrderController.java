package com.trading.controller;

import com.trading.dto.CompositeOrderRequest;
import com.trading.dto.OrderRequest;
import com.trading.dto.OrderResponse;
import com.trading.entity.CompositeOrderStatus;
import com.trading.service.CompositeOrderService;
import com.trading.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Endpoints for managing trading orders")
public class OrderController {

  private final OrderService orderService;
  private final CompositeOrderService compositeOrderService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Place a new order")
  public OrderResponse placeOrder(@Valid @RequestBody OrderRequest orderRequest) {
    return orderService.placeOrder(orderRequest);
  }

  @DeleteMapping("/{orderId}")
  @Operation(summary = "Cancel an existing order")
  public void cancelOrder(@PathVariable UUID orderId) {
    orderService.cancelOrder(orderId);
  }

  @GetMapping("/{orderId}")
  @Operation(summary = "Get order details")
  public OrderResponse getOrder(@PathVariable UUID orderId) {
    return orderService.getOrder(orderId);
  }

  @PostMapping("/composite")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a composite order")
  public UUID createCompositeOrder(@RequestBody CompositeOrderRequest request) {
    return compositeOrderService.createCompositeOrder(request.instruments(), request.traderId());
  }

  @GetMapping("/composite/{compositeId}/status")
  @Operation(summary = "Get composite order status")
  public CompositeOrderStatus getCompositeStatus(@PathVariable UUID compositeId) {
    return compositeOrderService.getCompositeStatus(compositeId);
  }
}
