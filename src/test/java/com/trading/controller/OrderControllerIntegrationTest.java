package com.trading.controller;

import static com.trading.entity.OrderDirection.BUY;
import static com.trading.entity.OrderDirection.SELL;
import static com.trading.entity.OrderType.LIMIT;
import static com.trading.entity.OrderType.MARKET;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_EACH_TEST_METHOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.DELETE;

import com.trading.dto.CompositeInstrumentRequest;
import com.trading.dto.CompositeOrderRequest;
import com.trading.dto.OrderRequest;
import com.trading.dto.OrderResponse;
import com.trading.entity.*;
import com.trading.repository.TradeRepository;
import com.trading.service.CompositeOrderService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY, refresh = BEFORE_EACH_TEST_METHOD)
class OrderControllerIntegrationTest {

  public static final String ORDERS = "/api/orders";
  public static final String ORDERS_COMPOSITE_STATUS = "/api/orders/composite/%s/status";
  public static final String ORDERS_COMPOSITE = "/api/orders/composite";
  @Autowired private TestRestTemplate restTemplate;

  @Autowired private CompositeOrderService compositeOrderService;

  @Autowired private TradeRepository tradeRepository;

  @Test
  void placeAndCancelOrder_ShouldWorkCorrectly() {
    OrderRequest request =
        new OrderRequest("TRADER1", "AAPL", BUY, LIMIT, 100, new BigDecimal("150.0"));
    OrderResponse response = placeOrder(request);
    assertNotNull(response.orderId());
    assertEquals(OrderStatus.OPEN, response.status());

    OrderResponse fetchedOrder = getOrder(response.orderId());
    assertEquals(response.orderId(), fetchedOrder.orderId());

    restTemplate.delete(ORDERS + "/" + response.orderId());

    OrderResponse canceledOrder = getOrder(response.orderId());
    assertEquals(OrderStatus.CANCELLED, canceledOrder.status());
  }

  @Test
  @SneakyThrows
  void placeInvalidOrder_ShouldReturnBadRequest() {
    var invalidRequest = new OrderRequest("", "", null, null, -100, new BigDecimal("-1.0"));
    var response = restTemplate.postForEntity(ORDERS, invalidRequest, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    JSONAssert.assertEquals(
        """
                        {
                          "quantity": "Minimum quantity is 1",
                          "price": "Price must be positive when specified",
                          "instrumentSymbol": "Instrument symbol is required",
                          "traderId": "Trader ID is required",
                          "type": "Order type is required (MARKET or LIMIT)",
                          "direction": "Direction of order is required (BUY or SELL)"
                        }
                    """,
        response.getBody(),
        false);
  }

  @Test
  void shouldMatchBuyAndSellOrders() {
    var buyRequest = new OrderRequest("TRADER1", "AAPL", BUY, LIMIT, 100, new BigDecimal("150.0"));
    var buyResponse = placeOrder(buyRequest);

    var sellRequest =
        new OrderRequest("TRADER2", "AAPL", SELL, LIMIT, 100, new BigDecimal("150.0"));
    var sellResponse = placeOrder(sellRequest);

    assertOrderStatus(buyResponse.orderId(), OrderStatus.FILLED);
    assertOrderStatus(sellResponse.orderId(), OrderStatus.FILLED);
  }

  @Test
  void shouldHandlePartialFills() {
    // Place large buy order
    var buyOrder =
        placeOrder(new OrderRequest("TRADER1", "AAPL", BUY, LIMIT, 500, new BigDecimal("150.0")));

    // Place smaller sell order
    var sellOrder1 =
        placeOrder(new OrderRequest("TRADER2", "AAPL", SELL, LIMIT, 200, new BigDecimal("150.0")));

    // Verify partial fill
    assertOrderStatus(buyOrder.orderId(), OrderStatus.PARTIALLY_FILLED);
    assertEquals(300, getOrderQuantity(buyOrder.orderId()));

    // Place second sell order
    var sellOrder2 =
        placeOrder(new OrderRequest("TRADER3", "AAPL", SELL, LIMIT, 300, new BigDecimal("150.0")));

    // Verify complete fill
    assertOrderStatus(buyOrder.orderId(), OrderStatus.FILLED);
    assertOrderStatus(sellOrder2.orderId(), OrderStatus.FILLED);
  }

  @Test
  void shouldPrioritizeOrdersByPrice() {
    placeOrder(new OrderRequest("TRADER1", "AAPL", BUY, LIMIT, 100, new BigDecimal("145.0")));
    placeOrder(new OrderRequest("TRADER2", "AAPL", BUY, LIMIT, 100, new BigDecimal("150.0")));
    placeOrder(new OrderRequest("TRADER3", "AAPL", BUY, LIMIT, 100, new BigDecimal("148.0")));

    var sellOrder =
        placeOrder(new OrderRequest("TRADER4", "AAPL", SELL, LIMIT, 100, new BigDecimal("148.0")));

    var trades = tradeRepository.findAll();
    assertEquals(1, trades.size(), "Should execute one trade");

    var trade = trades.getFirst();
    assertEquals(
        new BigDecimal("150.0").stripTrailingZeros(),
        trade.getPrice().stripTrailingZeros(),
        "Trade should execute at best buy price");
    assertEquals(100, trade.getQuantity());

    assertEquals(BUY, trade.getBuyOrder().getDirection());
    assertEquals(
        new BigDecimal("150.0").stripTrailingZeros(),
        trade.getBuyOrder().getPrice().stripTrailingZeros());
    assertOrderStatus(sellOrder.orderId(), OrderStatus.FILLED);
  }

  @Test
  void shouldCompleteCompositeOrderWhenAllUnderlyingFilled() {
    // place composite order
    var instruments =
        List.of(
            new CompositeInstrumentRequest("AAPL", 100, BUY, LIMIT, new BigDecimal("150.0")),
            new CompositeInstrumentRequest("MSFT", 200, BUY, LIMIT, new BigDecimal("300.0")));
    var request = new CompositeOrderRequest(instruments, "TRADER1");
    UUID compositeId = restTemplate.postForObject(ORDERS_COMPOSITE, request, UUID.class);
    // check composite order status
    var status =
        getForObject(ORDERS_COMPOSITE_STATUS.formatted(compositeId), CompositeOrderStatus.class);
    assertEquals(CompositeOrderStatus.PENDING, status);

    // place AAPL sell order
    placeOrder(new OrderRequest("TRADER2", "AAPL", SELL, LIMIT, 100, new BigDecimal("150.0")));

    // check composite order status
    status =
        getForObject(ORDERS_COMPOSITE_STATUS.formatted(compositeId), CompositeOrderStatus.class);
    assertEquals(CompositeOrderStatus.PARTIALLY_FILLED, status);

    // place MSFL sell order
    placeOrder(new OrderRequest("TRADER3", "MSFT", SELL, LIMIT, 200, new BigDecimal("300.0")));
    status =
        getForObject(ORDERS_COMPOSITE_STATUS.formatted(compositeId), CompositeOrderStatus.class);
    assertEquals(CompositeOrderStatus.FILLED, status);
  }

  @Test
  void shouldNotCancelFilledOrder() {
    // Place an order and ensure it's filled
    var request1 = new OrderRequest("TRADER1", "AAPL", BUY, LIMIT, 100, new BigDecimal("150.0"));
    var response1 = placeOrder(request1);
    assertNotNull(response1.orderId());

    var request2 = new OrderRequest("TRADER2", "AAPL", SELL, LIMIT, 100, new BigDecimal("150.0"));
    var response2 = placeOrder(request2);

    // Try to cancel the order after it's filled
    var uri = ORDERS + "/" + response1.orderId();
    var exchange = restTemplate.exchange(uri, DELETE, null, String.class);
    assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    assertEquals("Cannot cancel a non-open order", exchange.getBody());
  }

  @Test
  void shouldReturnErrorWhenCancelingNonExistingOrder() {
    UUID nonExistentOrderId = UUID.randomUUID();
    var response =
        restTemplate.exchange(ORDERS + "/" + nonExistentOrderId, DELETE, null, String.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertThat(response.getBody()).startsWith("Order not found with ID:");
  }

  @Test
  void shouldHandleMarketOrders() {
    // Place buy limit order (direction=BUY, type=LIMIT)
    var limitBuy = new OrderRequest("TRADER1", "AAPL", BUY, LIMIT, 100, new BigDecimal("150.0"));
    placeOrder(limitBuy);

    // Place market sell order (direction=SELL, type=MARKET)
    var marketSell = new OrderRequest("TRADER2", "AAPL", SELL, MARKET, 100, null);
    OrderResponse response = placeOrder(marketSell);

    // Verify execution
    assertOrderStatus(response.orderId(), OrderStatus.FILLED);
    var trades = tradeRepository.findAll();
    assertEquals(1, trades.size(), "Should execute one trade");

    var trade = trades.getFirst();
    assertEquals(response.orderId(), trade.getSellOrder().getId().toString());
    assertEquals(
        new BigDecimal("150.0").stripTrailingZeros(), trade.getPrice().stripTrailingZeros());
  }

  private OrderResponse placeOrder(OrderRequest request) {
    return restTemplate.postForObject(ORDERS, request, OrderResponse.class);
  }

  private void assertOrderStatus(String orderId, OrderStatus expectedStatus) {
    OrderResponse order = getOrder(orderId);
    assertEquals(expectedStatus, order.status());
  }

  private Integer getOrderQuantity(String orderId) {
    return getOrder(orderId).quantity();
  }

  private OrderResponse getOrder(String orderId) {
    return restTemplate.getForObject("/api/orders/" + orderId, OrderResponse.class);
  }

  private <T> T getForObject(String uri, Class<T> clazz) {
    return restTemplate.getForObject(uri, clazz);
  }
}
