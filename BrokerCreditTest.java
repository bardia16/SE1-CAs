package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.config.MockedJMSTestConfig;
import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.service.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@SpringBootTest
@Import(MockedJMSTestConfig.class)
@DirtiesContext
public class BrokerCreditTest {
    private Security security;
    private Broker buy_broker, sell_broker;
    private Shareholder shareholder;
    private OrderBook orderBook;
    private List<Order> orders;
    @Autowired
    private Matcher matcher;
    private long initial_credit;

    @BeforeEach
    void setupOrderBook() {
        initial_credit = 10_000_000L;

        security = Security.builder().build();
        buy_broker = Broker.builder().credit(initial_credit).build();
        sell_broker = Broker.builder().credit(initial_credit).build();
        shareholder = Shareholder.builder().build();
        shareholder.incPosition(security, 100_000);
        orderBook = security.getOrderBook();
        orders = Arrays.asList(
                new Order(1, security, Side.BUY, 304, 15700, buy_broker, shareholder),
                new Order(2, security, Side.BUY, 43, 15500, buy_broker, shareholder),
                new Order(3, security, Side.BUY, 445, 15450, buy_broker, shareholder),
                new Order(4, security, Side.BUY, 526, 15450, buy_broker, shareholder),
                new Order(5, security, Side.BUY, 1000, 15400, buy_broker, shareholder),
                new IcebergOrder(12, security, Side.BUY, 500, 15300, buy_broker, shareholder, 100),
                new Order(6, security, Side.SELL, 350, 15800, sell_broker, shareholder),
                new Order(7, security, Side.SELL, 285, 15810, sell_broker, shareholder),
                new Order(8, security, Side.SELL, 800, 15810, sell_broker, shareholder),
                new Order(9, security, Side.SELL, 340, 15820, sell_broker, shareholder),
                new Order(10, security, Side.SELL, 65, 15820, sell_broker, shareholder),
                new IcebergOrder(11, security, Side.SELL, 500, 15830, sell_broker, shareholder, 100)
        );
        orders.forEach(order -> orderBook.enqueue(order));
    }

    //New buy orders tests
    @Test
    void check_traded_value_new_buy_order_matches_completely_with_part_of_the_first_sell() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.BUY, 550, 15840, buy_broker.getBrokerId(), 0, 0);
        long traded_value = 350 * 15800 + 200 * 15810;
        security.newOrder(enterOrderRq, buy_broker, shareholder, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit - traded_value);
    }

    @Test
    void check_traded_value_new_buy_order_matches_completely_with_two_sells() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.BUY, 100, 15840, buy_broker.getBrokerId(), 0, 0);
        long traded_value = 100 * 15800;
        security.newOrder(enterOrderRq, buy_broker, shareholder, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit - traded_value);
    }

    @Test
    void check_traded_value_new_buy_order_matches_partially_with_one_sell() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.BUY, 400, 15800, buy_broker.getBrokerId(), 0, 0);
        long traded_value = 400 * 15800;
        security.newOrder(enterOrderRq, buy_broker, shareholder, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit - traded_value);
    }

    @Test
    void check_traded_value_new_buy_order_does_not_match_with_sells() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.BUY, 400, 15700, buy_broker.getBrokerId(), 0, 0);
        long traded_value = 400 * 15700;
        security.newOrder(enterOrderRq, buy_broker, shareholder, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit - traded_value);
    }

    @Test
    void check_traded_value_new_buy_order_does_match_with_some_sells_but_rollbacks_not_enough_credit_to_buy() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.BUY, 10000, 20000, buy_broker.getBrokerId(), 0, 0);
        long traded_value = 0;
        security.newOrder(enterOrderRq, buy_broker, shareholder, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit - traded_value);
    }

    @Test
    void check_traded_value_new_buy_order_does_match_with_some_sells_but_rollbacks_not_enough_credit_to_enter_orderbook() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.BUY, 10000, 15800, buy_broker.getBrokerId(), 0, 0);
        long traded_value = 0;
        security.newOrder(enterOrderRq, buy_broker, shareholder, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit - traded_value);
    }


    //New sell order tests

    @Test
    void check_traded_value_new_sell_order_matches_completely_with_part_of_the_first_buy() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.SELL, 300, 15600, sell_broker.getBrokerId(), 0, 0);
        long traded_value = 300 * 15700;
        security.newOrder(enterOrderRq, sell_broker, shareholder, matcher);
        assertThat(sell_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_new_sell_order_matches_completely_with_two_buys() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.SELL, 400, 15500, sell_broker.getBrokerId(), 0, 0);
        long traded_value = 304 * 15700 + 43 * 15500;
        security.newOrder(enterOrderRq, sell_broker, shareholder, matcher);
        assertThat(sell_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_new_sell_order_matches_partially_with_one_buy() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.SELL, 400, 15650, sell_broker.getBrokerId(), 0, 0);
        long traded_value = 304 * 15700;
        security.newOrder(enterOrderRq, sell_broker, shareholder, matcher);
        assertThat(sell_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_new_sell_order_does_not_match_with_buy() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.SELL, 400, 15800, sell_broker.getBrokerId(), 0, 0);
        long traded_value = 0;
        security.newOrder(enterOrderRq, sell_broker, shareholder, matcher);
        assertThat(sell_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_new_sell_order_does_match_with_some_buys_but_one_buyer_rollbacks_not_enough_credit_to_buy() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.SELL, 10000, 20000, buy_broker.getBrokerId(), 0, 0);
        long traded_value = 0;
        security.newOrder(enterOrderRq, buy_broker, shareholder, matcher);
        assertThat(sell_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    //Cancel order tests

    @Test
    void check_traded_value_delete_existing_sell_order() {
        DeleteOrderRq deleteOrderRq = new DeleteOrderRq(1, security.getIsin(), Side.SELL, 10);
        assertThatNoException().isThrownBy(() -> security.deleteOrder(deleteOrderRq));
        long current_credit = sell_broker.getCredit();
        long traded_value = 0;
        assertThat(current_credit).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_delete_existing_buy_order() {
        DeleteOrderRq deleteOrderRq = new DeleteOrderRq(1, security.getIsin(), Side.BUY, 1);
        assertThatNoException().isThrownBy(() -> security.deleteOrder(deleteOrderRq));
        long current_credit = buy_broker.getCredit();
        long traded_value = 304 * 15700;
        assertThat(current_credit).isEqualTo(initial_credit + traded_value);
    }

    //Amend order tests

    @Test
    void check_traded_value_update_existing_buy_order_that_matches_partially_with_sell()
            throws InvalidRequestException {
        EnterOrderRq updateOrderRq = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 1,
                LocalDateTime.now(), Side.BUY, 200, 15800, buy_broker.getBrokerId(), 0, 0);

        security.updateOrder(updateOrderRq, matcher);
        long current_credit = buy_broker.getCredit();
        long traded_value = 304 * 15700 - 200 * 15800;
        assertThat(current_credit).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_update_existing_buy_order_remains_in_orderbook()
            throws InvalidRequestException {
        EnterOrderRq updateOrderRq = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 1,
                LocalDateTime.now(), Side.BUY, 200, 15600, buy_broker.getBrokerId(), 0, 0);

        security.updateOrder(updateOrderRq, matcher);
        long current_credit = buy_broker.getCredit();
        long traded_value = 304 * 15700 - 200 * 15600;
        assertThat(current_credit).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_update_existing_buy_order_matches_with_sells_but_rollbacks_not_enough_credit_to_buy()
            throws InvalidRequestException {
        EnterOrderRq updateOrderRq = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 1,
                LocalDateTime.now(), Side.BUY, 10000, 20000, buy_broker.getBrokerId(), 0, 0);

        security.updateOrder(updateOrderRq, matcher);
        long current_credit = buy_broker.getCredit();
        long traded_value = 0;
        assertThat(current_credit).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_update_existing_sell_order_matches_with_buys_but_one_buyer_rollbacks_not_enough_credit_to_buy()
            throws InvalidRequestException {
        EnterOrderRq updateOrderRqSell = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 6,
                LocalDateTime.now(), Side.SELL, 10000, 20000, sell_broker.getBrokerId(), 0, 0);

        EnterOrderRq updateOrderRqBuy = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 1,
                LocalDateTime.now(), Side.BUY, 10000, 20000, buy_broker.getBrokerId(), 0, 0);

        security.updateOrder(updateOrderRqSell, matcher);
        security.updateOrder(updateOrderRqBuy, matcher);
        long current_credit = sell_broker.getCredit();
        long traded_value = 0;
        assertThat(current_credit).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_update_existing_sell_order_matches_completely_with_buys()
            throws InvalidRequestException {
        EnterOrderRq updateOrderRq = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 6,
                LocalDateTime.now(), Side.SELL, 300, 15700, sell_broker.getBrokerId(), 0, 0);

        security.updateOrder(updateOrderRq, matcher);
        long current_credit = sell_broker.getCredit();
        long traded_value = 300 * 15700;
        assertThat(current_credit).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_update_existing_sell_order_remains_in_orderbook()
            throws InvalidRequestException {
        EnterOrderRq updateOrderRq = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 6,
                LocalDateTime.now(), Side.SELL, 300, 15900, sell_broker.getBrokerId(), 0, 0);

        security.updateOrder(updateOrderRq, matcher);
        long current_credit = sell_broker.getCredit();
        long traded_value = 0;
        assertThat(current_credit).isEqualTo(initial_credit + traded_value);
    }

    // Iceberg order tests

    @Test
    void check_traded_value_new_buy_iceberg_order_matches_partially_with_sells() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.BUY, 500, 15805, buy_broker.getBrokerId(), 0, 100);
        long traded_value = 350 * 15800 + 150 * 15805;
        security.newOrder(enterOrderRq, buy_broker, shareholder, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit - traded_value);
    }

    @Test
    void check_traded_value_new_buy_iceberg_order_does_not_match_enters_orderbook() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.BUY, 500, 15600, buy_broker.getBrokerId(), 0, 100);
        long traded_value = 15600*500;
        security.newOrder(enterOrderRq, buy_broker, shareholder, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit - traded_value);
    }


    @Test
    void check_traded_value_new_sell_iceberg_order_matches_partially_with_buys() {
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 3,
                LocalDateTime.now(), Side.SELL, 500, 15700, sell_broker.getBrokerId(), 0, 100);
        long traded_value = 304 * 15700;
        security.newOrder(enterOrderRq, sell_broker, shareholder, matcher);
        assertThat(sell_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_update_buy_iceberg_order_matches_partially_with_sells()
            //The error is because the order status should be updated to "New" before entering orderbook
            throws InvalidRequestException {
        EnterOrderRq enterOrderRq = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 12,
                LocalDateTime.now(), Side.BUY, 450, 15805, buy_broker.getBrokerId(), 0, 100);
        long traded_value = 500 * 15300 - (350 * 15800 + 100 * 15805);
        security.updateOrder(enterOrderRq, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_update_buy_iceberg_order_does_not_match_enters_orderbook()
            //The error is because the order status should be updated to "New" before entering orderbook
            throws InvalidRequestException {
        EnterOrderRq enterOrderRq = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 12,
                LocalDateTime.now(), Side.BUY, 450, 15600, buy_broker.getBrokerId(), 0, 100);
        long traded_value = 500 * 15300 - (450 * 15600);
        security.updateOrder(enterOrderRq, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_update_sell_iceberg_order_matches_partially_with_buys()
            throws InvalidRequestException {
        EnterOrderRq enterOrderRq = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 11,
                LocalDateTime.now(), Side.SELL, 450, 15600, sell_broker.getBrokerId(), 0, 100);
        long traded_value = 304*15700;
        security.updateOrder(enterOrderRq, matcher);
        assertThat(sell_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_update_buy_iceberg_order_matches_with_sells_but_rollbacks_not_enough_credit_to_buy()
            throws InvalidRequestException {
        EnterOrderRq enterOrderRq = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 12,
                LocalDateTime.now(), Side.BUY, 10000, 200000, buy_broker.getBrokerId(), 0, 100);
        long traded_value = 0;
        security.updateOrder(enterOrderRq, matcher);
        assertThat(buy_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_update_sell_iceberg_order_matches_with_buys_but_one_buyer_rollbacks_not_enough_credit_to_buy()
            throws InvalidRequestException {
        EnterOrderRq enterOrderRq = EnterOrderRq.createUpdateOrderRq(1, security.getIsin(), 12,
                LocalDateTime.now(), Side.BUY, 10000, 200000, buy_broker.getBrokerId(), 0, 100);
        long traded_value = 0;
        security.updateOrder(enterOrderRq, matcher);
        assertThat(sell_broker.getCredit()).isEqualTo(initial_credit + traded_value);
    }

    @Test
    void check_traded_value_new_sell_order_matches_with_iceberg_buy_order() {
        EnterOrderRq enterOrderRqIceberg = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 11,
                LocalDateTime.now(), Side.BUY, 1000, 15730, buy_broker.getBrokerId(),
                shareholder.getShareholderId(), 100);
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(2, security.getIsin(), 12,
                LocalDateTime.now(), Side.SELL, 250, 15700, sell_broker.getBrokerId(),
                shareholder.getShareholderId(), 0);
        security.newOrder(enterOrderRqIceberg, buy_broker, shareholder, matcher);
        security.newOrder(enterOrderRq, sell_broker, shareholder, matcher);

        long current_credit = sell_broker.getCredit();
        long trade_value = 250 * 15700;
        assertThat(current_credit).isEqualTo(initial_credit + trade_value);
    }

    @Test
    void check_traded_value_new_buy_order_matches_with_iceberg_sell_order() {
        EnterOrderRq enterOrderRqIceberg = EnterOrderRq.createNewOrderRq(1, security.getIsin(), 11,
                LocalDateTime.now(), Side.SELL, 1000, 15750, sell_broker.getBrokerId(),
                shareholder.getShareholderId(), 100);
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(2, security.getIsin(), 12,
                LocalDateTime.now(), Side.BUY, 400, 15800, buy_broker.getBrokerId(),
                shareholder.getShareholderId(), 0);
        security.newOrder(enterOrderRqIceberg, sell_broker, shareholder, matcher);
        security.newOrder(enterOrderRq, buy_broker, shareholder, matcher);

        long current_credit = buy_broker.getCredit();
        long trade_value = 400 * 15750;
        assertThat(current_credit).isEqualTo(initial_credit - trade_value);
    }

}