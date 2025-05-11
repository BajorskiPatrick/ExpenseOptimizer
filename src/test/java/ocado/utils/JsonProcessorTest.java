package ocado.utils;

import ocado.model.Order;
import ocado.model.PaymentMethod;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class JsonProcessorTest {
    @Test
    void testGetOrdersAsListWhenWrongFile() {
        File fileOne = new File("src/test/resources/wrongpath.json");
        assertThrows(FileNotFoundException.class, () -> JsonProcessor.getOrdersAsList(fileOne));

        File fileTwo = new File("src/test/resources/wrongextension.txt");
        assertThrows(IllegalArgumentException.class, () -> JsonProcessor.getOrdersAsList(fileTwo));
    }

    @Test
    void testGetOrdersAsListWhenEmpty() throws IOException {
        File fileOne = new File("src/test/resources/emptyorders.json");
        List<Order> orders = JsonProcessor.getOrdersAsList(fileOne);
        assertTrue(orders.isEmpty());

        File fileTwo = new File("src/test/resources/emptyfile.json");
        List<Order> ordersTwo = JsonProcessor.getOrdersAsList(fileTwo);
        assertTrue(ordersTwo.isEmpty());
    }

    @Test
    void testGetOrdersAsList() throws IOException {
        File file = new File("src/test/resources/oneorder.json");
        List<Order> order = JsonProcessor.getOrdersAsList(file);
        assertEquals(1, order.size());
        assertEquals("ZAM1", order.getFirst().getId());
        assertEquals(200.0, order.getFirst().getValue());

        List<String> promotions = order.getFirst().getPromotions();
        assertEquals(2, promotions.size());
        assertTrue(promotions.contains("SuperKarta"));
        assertTrue(promotions.contains("MegaBank"));
    }

    @Test
    void testGetPaymentsAsMapWhenWrongFile() throws IOException {
        File fileOne = new File("src/test/resources/wrongpath.json");
        assertThrows(FileNotFoundException.class, () -> JsonProcessor.getPaymentsAsMap(fileOne, new ArrayList<>()));

        File fileTwo = new File("src/test/resources/wrongextension.txt");
        assertThrows(IllegalArgumentException.class, () -> JsonProcessor.getPaymentsAsMap(fileTwo, new ArrayList<>()));
    }

    @Test
    void testGetPaymentsAsMapWhenEmpty() throws IOException {
        File file = new File("src/test/resources/emptypaymentmethods.json");
        Map<String, PaymentMethod> paymentMethodsOne = JsonProcessor.getPaymentsAsMap(file, new ArrayList<>());
        assertTrue(paymentMethodsOne.isEmpty());

        File fileTwo = new File("src/test/resources/emptyfile.json");
        Map<String, PaymentMethod> paymentMethodsTwo = JsonProcessor.getPaymentsAsMap(fileTwo, new ArrayList<>());
        assertTrue(paymentMethodsTwo.isEmpty());
    }

    @Test
    void testGetPaymentsAsMap() throws IOException {
        File fileOrder = new File("src/test/resources/oneorder.json");
        List<Order> orders = JsonProcessor.getOrdersAsList(fileOrder);

        File filePayment = new File("src/test/resources/onepayment.json");
        Map<String, PaymentMethod> paymentMethods = JsonProcessor.getPaymentsAsMap(filePayment, orders);

        assertEquals(1, paymentMethods.size());
        assertTrue(paymentMethods.containsKey("SuperKarta"));

        PaymentMethod paymentMethod = paymentMethods.get("SuperKarta");
        assertEquals("SuperKarta", paymentMethod.getId());
        assertEquals(10, paymentMethod.getDiscount());
        assertEquals(250.0, paymentMethod.getLimit());
        assertEquals(1, paymentMethod.getOrdersAmount());
        assertEquals(0.0, paymentMethod.getSpending());
    }
}
