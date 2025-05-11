package ocado.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelTest {
    @Test
    void testOrderUsePaymentMethod() {
        Order order = new Order();
        assertTrue(order.getUsedPaymentsMethods().isEmpty());

        order.usePaymentsMethod("test", 1.0);
        assertTrue(order.getUsedPaymentsMethods().containsKey("test"));
        assertEquals(1.0, order.getUsedPaymentsMethods().get("test"));
    }

    @Test
    void testPaymentMethodIncrementOrdersAmount() {
        PaymentMethod paymentMethod = new PaymentMethod();
        assertEquals(0, paymentMethod.getOrdersAmount());

        paymentMethod.incrementOrdersAmount();
        assertEquals(1, paymentMethod.getOrdersAmount());
    }

    @Test
    void testPaymentMethodDecrementOrdersAmount() {
        PaymentMethod paymentMethod = new PaymentMethod();
        assertEquals(0, paymentMethod.getOrdersAmount());

        paymentMethod.decrementOrdersAmount();
        assertEquals(0, paymentMethod.getOrdersAmount());

        paymentMethod.setOrdersAmount(5);
        paymentMethod.decrementOrdersAmount();
        assertEquals(4, paymentMethod.getOrdersAmount());
    }

    @Test
    void testPaymentMethodCorrectSpend() {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setLimit(20);
        paymentMethod.spend(10);
        assertEquals(10, paymentMethod.getLimit());
        assertEquals(10, paymentMethod.getSpending());
    }

    @Test
    void testPaymentMethodSpendWrongAmount() {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setLimit(20);
        assertThrows(IllegalArgumentException.class, () -> paymentMethod.spend(21));
        assertThrows(IllegalArgumentException.class, () -> paymentMethod.spend(-1));
    }

    @Test
    void testPaymentMethodGetMoneyBack() {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setSpending(20);
        paymentMethod.getMoneyBack(10);
        assertEquals(10, paymentMethod.getSpending());
        assertEquals(10, paymentMethod.getLimit());
    }

    @Test
    void testPaymentMethodGetMoneyBackWrongAmount() {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setSpending(20);
        assertThrows(IllegalArgumentException.class, () -> paymentMethod.getMoneyBack(-1));
        assertThrows(IllegalArgumentException.class, () -> paymentMethod.getMoneyBack(21));
    }
}
