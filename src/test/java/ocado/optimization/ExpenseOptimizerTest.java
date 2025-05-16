package ocado.optimization;

import ocado.model.Order;
import ocado.model.PaymentMethod;
import ocado.utils.JsonProcessor;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseOptimizerTest {
    @Test
    void testOptimize() throws IOException {
        File fileOrders = new File("src/test/resources/orders.json");
        File filePayments = new File("src/test/resources/paymentmethods.json");
        List<Order> orders = JsonProcessor.getOrdersAsList(fileOrders);
        Map<String, PaymentMethod> payments = JsonProcessor.getPaymentsAsMap(filePayments, orders);
        PaymentMethod pointsMethod = payments.remove("PUNKTY");

        ExpenseOptimizer optimizer = new ExpenseOptimizer(orders, payments, pointsMethod);

        boolean result = optimizer.optimize();
        assertTrue(result);
        assertEquals(new BigDecimal("170.00"), payments.get("MegaBank").getSpending());
        assertEquals(new BigDecimal("150.00"), payments.get("SuperKarta").getSpending());
        assertEquals(new BigDecimal("150.00"), pointsMethod.getSpending());
        assertEquals(new BigDecimal("0.00"), payments.get("ZwyklaKarta").getSpending());
    }
}
