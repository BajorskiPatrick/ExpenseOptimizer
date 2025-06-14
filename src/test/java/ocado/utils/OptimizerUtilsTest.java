package ocado.utils;

import ocado.model.Order;
import ocado.model.PaymentMethod;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptimizerUtilsTest {
    private static OptimizerUtils utils;
    private static List<PaymentMethod> methods;

    @BeforeAll
    static void setUp() throws IOException {
        File fileOrders = new File("src/test/resources/orders.json");
        File filePayments = new File("src/test/resources/paymentmethods.json");
        Map<String, PaymentMethod> map =
                JsonProcessor.getPaymentsAsMap(filePayments, JsonProcessor.getOrdersAsList(fileOrders));
        map.remove("PUNKTY");

        utils = new OptimizerUtils();
        methods = map.values().stream().toList();
    }

    @Test
    void testPayWholeOrder() {
        PaymentMethod pm = new PaymentMethod("", "", "");
        Order order = new Order("", "200", new ArrayList<>());
        pm.setLimit(new BigDecimal("200.00"));
        pm.setDiscount(new BigDecimal("20.00"));

        utils.pay(pm, order);
        assertEquals(new BigDecimal("160.00"), pm.getSpending());
    }

    @Test
    void testPayProvidedAmount() {
        PaymentMethod pm = new PaymentMethod("", "", "");
        pm.setLimit(new BigDecimal("200.00"));
        utils.pay(pm, new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), pm.getSpending());
    }

    @Test
    void testFindWithMinLimit() {
        List<PaymentMethod> min = utils.findWithMinLimit(methods);
        assertEquals(1, min.size());
        assertEquals("MegaBank", min.getFirst().getId());
    }

    @Test
    void findWithMinLimitWhenListEmpty() {
        List<PaymentMethod> min = utils.findWithMinLimit(new ArrayList<>());
        assertTrue(min.isEmpty());
    }

    @Test
    void testFindWithMaxLimit() {
        List<PaymentMethod> max = utils.findWithMaxLimit(methods);
        assertEquals(1, max.size());
        assertEquals("ZwyklaKarta", max.getFirst().getId());
    }

    @Test
    void findWithMaxLimitWhenListEmpty() {
        List<PaymentMethod> max = utils.findWithMaxLimit(new ArrayList<>());
        assertTrue(max.isEmpty());
    }

    @Test
    void findLowestDiscountMethods() {
        List<PaymentMethod> lowest = utils.findLowestDiscountMethods(methods);
        assertEquals(1, lowest.size());
        assertEquals("ZwyklaKarta", lowest.getFirst().getId());
    }

    @Test
    void findLowestDiscountMethodsWhenListEmpty() {
        List<PaymentMethod> lowest = utils.findLowestDiscountMethods(new ArrayList<>());
        assertTrue(lowest.isEmpty());
    }

    @Test
    void testFindOptimalCardWithoutDraw() {
        PaymentMethod pm = utils.findOptimalCard(methods);
        assertEquals("ZwyklaKarta", pm.getId());
    }

    @Test
    void findOptimalCardWhileDraw() {
        List<PaymentMethod> currMethods = methods.stream().filter(pm -> !pm.getId().equals("ZwyklaKarta")).toList();
        PaymentMethod pm = utils.findOptimalCard(currMethods);
        assertEquals("MegaBank", pm.getId());
    }

    @Test
    void testFindBestCardToPayRestWhenListEmpty() {
        PaymentMethod pm = utils.findBestCardToPayRest(new ArrayList<>());
        assertNull(pm);
    }

    @Test
    void testFindBestCardToPayRestWhenListWithSizeOne() {
        PaymentMethod pm = utils.findBestCardToPayRest(List.of(methods.getFirst()));
        assertEquals(methods.getFirst().getId(), pm.getId());
    }

    @Test
    void testFindBestCardToPayRestWhenOneWithOrderAmountZero() {
        PaymentMethod pm = utils.findBestCardToPayRest(methods);
        assertEquals("ZwyklaKarta", pm.getId());
    }

    @Test
    void testFindBestCardToPayRestWhenOneWithLowestDiscount() {
        List<PaymentMethod> currMethods = methods.stream().filter(pm -> !pm.getId().equals("ZwyklaKarta")).toList();
        PaymentMethod pm = utils.findBestCardToPayRest(currMethods);
        assertEquals("SuperKarta", pm.getId());
    }
}
