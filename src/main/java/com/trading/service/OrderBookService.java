package com.trading.service;

import com.trading.entity.*;
import com.trading.repository.InstrumentRepository;
import com.trading.repository.OrderRepository;
import com.trading.repository.TradeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderBookService {

  private final OrderRepository orderRepository;

  private final TradeRepository tradeRepository;

  private final InstrumentRepository instrumentRepository;

  public InstrumentEntity getOrCreateInstrument(String symbol) {
    return instrumentRepository
        .findBySymbol(symbol)
        .orElseGet(
            () -> {
              InstrumentEntity newInstrument = new InstrumentEntity();
              newInstrument.setSymbol(symbol);
              return instrumentRepository.save(newInstrument);
            });
  }

  @Transactional
  public OrderEntity processOrder(OrderEntity newOrder) {

    InstrumentEntity instrument = getOrCreateInstrument(newOrder.getInstrument().getSymbol());
    newOrder.setInstrument(instrument);
    newOrder.setStatus(OrderStatus.OPEN);
    newOrder.setRemainingQuantity(newOrder.getQuantity());

    OrderEntity savedOrder = orderRepository.save(newOrder);
    List<OrderEntity> matchingOrders;
    if (newOrder.getDirection() == OrderDirection.BUY) {
      matchingOrders = findMatchingSellOrders(instrument, newOrder.getPrice());
    } else {
      matchingOrders = findMatchingBuyOrders(instrument, newOrder.getPrice());
    }

    Iterator<OrderEntity> iterator = matchingOrders.iterator();
    while (iterator.hasNext() && savedOrder.getRemainingQuantity() > 0) {
      OrderEntity oppositeOrder = iterator.next();
      int tradeQuantity =
          Math.min(savedOrder.getRemainingQuantity(), oppositeOrder.getRemainingQuantity());
      BigDecimal tradePrice = determineTradePrice(newOrder, oppositeOrder);
      executeTrade(savedOrder, oppositeOrder, instrument, tradePrice, tradeQuantity);

      // Update remaining quantities
      savedOrder.setRemainingQuantity(savedOrder.getRemainingQuantity() - tradeQuantity);
      oppositeOrder.setRemainingQuantity(oppositeOrder.getRemainingQuantity() - tradeQuantity);

      // Update order statuses
      updateOrderStatus(savedOrder);
      updateOrderStatus(oppositeOrder);

      // Save changes
      orderRepository.save(savedOrder);
      orderRepository.save(oppositeOrder);

      // Update instrument's market price
      instrument.setMarketPrice(tradePrice);
      instrumentRepository.save(instrument);
    }

    if (savedOrder.getRemainingQuantity() < 0) {
      savedOrder.setStatus(OrderStatus.FILLED);
      orderRepository.save(savedOrder);
    }
    return savedOrder;
  }

  /**
   * For a buy order, find sell orders with price <= maxPrice (if limit) or all (market) Sort by
   * price ascending, then time ascending
   *
   * @param instrument
   * @param maxPrice
   * @return
   */
  private List<OrderEntity> findMatchingSellOrders(
      InstrumentEntity instrument, BigDecimal maxPrice) {
    final var activeStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED);
    final var sellOrders =
        orderRepository.findByInstrumentAndDirectionAndStatusInOrderByPriceAscCreatedAtAsc(
            instrument, OrderDirection.SELL, activeStatuses);

    if (maxPrice != null) {
      return sellOrders.stream()
          .filter(order -> order.getPrice().compareTo(maxPrice) <= 0)
          .toList();
    } else {
      return sellOrders;
    }
  }

  /**
   * For a sell order, find buy orders with price >= minPrice (if limit) or all (market) Sort by
   * price descending, then time ascending
   *
   * @param instrument
   * @param minPrice
   * @return
   */
  private List<OrderEntity> findMatchingBuyOrders(
      InstrumentEntity instrument, BigDecimal minPrice) {
    List<OrderStatus> activeStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED);
    List<OrderEntity> buyOrders =
        orderRepository.findByInstrumentAndDirectionAndStatusInOrderByPriceDescCreatedAtAsc(
            instrument, OrderDirection.BUY, activeStatuses);
    if (minPrice != null) {
      return buyOrders.stream().filter(order -> order.getPrice().compareTo(minPrice) >= 0).toList();
    } else {
      return buyOrders;
    }
  }

  private BigDecimal determineTradePrice(OrderEntity newOrder, OrderEntity oppositeOrder) {
    if (newOrder.getPrice() == null) {
      return oppositeOrder.getPrice();
    } else if (oppositeOrder.getPrice() == null) {
      return newOrder.getPrice();
    } else {
      return oppositeOrder.getPrice();
    }
  }

  private void executeTrade(
      OrderEntity newOrder,
      OrderEntity oppositeOrder,
      InstrumentEntity instrument,
      BigDecimal price,
      int quantity) {
    TradeEntity trade = new TradeEntity();
    trade.setBuyOrder(newOrder.getDirection() == OrderDirection.BUY ? newOrder : oppositeOrder);
    trade.setSellOrder(newOrder.getDirection() == OrderDirection.SELL ? newOrder : oppositeOrder);
    trade.setInstrument(instrument);
    trade.setPrice(price);
    trade.setQuantity(quantity);
    trade.setTimestamp(Instant.now());
    tradeRepository.save(trade);
  }

  private void updateOrderStatus(OrderEntity order) {
    if (order.getRemainingQuantity() == 0) {
      order.setStatus(OrderStatus.FILLED);
    } else if (order.getRemainingQuantity() < order.getQuantity()) {
      order.setStatus(OrderStatus.PARTIALLY_FILLED);
    } else {
      order.setStatus(OrderStatus.OPEN);
    }
  }
}
