package com.trading.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
  public OrderNotFoundException(UUID orderNotFound) {
    super("Order not found with ID: " + orderNotFound);
  }
}
