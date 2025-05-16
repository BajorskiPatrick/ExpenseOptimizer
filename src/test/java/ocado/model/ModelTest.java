package ocado.model;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {
    @Test
    void testOrderUsePaymentMethod() {
        Order order = new Order("", "", new ArrayList<>());
        assertTrue(order.getUsedPaymentsMethods().isEmpty());

        order.usePaymentsMethod("test", new BigDecimal("1.00"));
        assertTrue(order.getUsedPaymentsMethods().containsKey("test"));
        assertEquals(new BigDecimal("1.00"), order.getUsedPaymentsMethods().get("test"));
    }

    @Test
    void testPaymentMethodIncrementOrdersAmount() {
        PaymentMethod paymentMethod = new PaymentMethod("", "", "");
        assertEquals(0, paymentMethod.getOrdersAmount());

        paymentMethod.incrementOrdersAmount();
        assertEquals(1, paymentMethod.getOrdersAmount());
    }

    @Test
    void testPaymentMethodDecrementOrdersAmount() {
        PaymentMethod paymentMethod = new PaymentMethod("", "", "");
        assertEquals(0, paymentMethod.getOrdersAmount());

        paymentMethod.decrementOrdersAmount();
        assertEquals(0, paymentMethod.getOrdersAmount());

        paymentMethod.setOrdersAmount(5);
        paymentMethod.decrementOrdersAmount();
        assertEquals(4, paymentMethod.getOrdersAmount());
    }

    @Test
    void testPaymentMethodCorrectSpend() {
        PaymentMethod paymentMethod = new PaymentMethod("", "", "");
        paymentMethod.setLimit(new BigDecimal("20.00"));
        paymentMethod.spend(new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), paymentMethod.getLimit());
        assertEquals(new BigDecimal("10.00"), paymentMethod.getSpending());
    }

    @Test
    void testPaymentMethodSpendWrongAmount() {
        PaymentMethod paymentMethod = new PaymentMethod("", "", "");
        paymentMethod.setLimit(new BigDecimal("20.00"));
        assertThrows(IllegalArgumentException.class, () -> paymentMethod.spend(new BigDecimal("21.00")));
        assertThrows(IllegalArgumentException.class, () -> paymentMethod.spend(new BigDecimal("-1.00")));
    }

    @Test
    void testPaymentMethodGetMoneyBack() {
        PaymentMethod paymentMethod = new PaymentMethod("", "", "");
        paymentMethod.setSpending(new BigDecimal("20.00"));
        paymentMethod.getMoneyBack(new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), paymentMethod.getSpending());
        assertEquals(new BigDecimal("10.00"), paymentMethod.getLimit());
    }

    @Test
    void testPaymentMethodGetMoneyBackWrongAmount() {
        PaymentMethod paymentMethod = new PaymentMethod("", "", "");
        paymentMethod.setSpending(new BigDecimal("20.00"));
        assertThrows(IllegalArgumentException.class, () -> paymentMethod.getMoneyBack(new BigDecimal("-1.00")));
        assertThrows(IllegalArgumentException.class, () -> paymentMethod.getMoneyBack(new BigDecimal("21.00")));
    }
}
