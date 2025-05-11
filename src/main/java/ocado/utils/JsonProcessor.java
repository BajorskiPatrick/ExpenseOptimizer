package ocado.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ocado.model.Order;
import ocado.model.PaymentMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonProcessor {
    public static List<Order> getOrdersAsList(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        return new ArrayList<>(mapper.readValue(file, new TypeReference<>() {
        }));
    }

    public static Map<String, PaymentMethod> getPaymentsAsMap(File file, List<Order> orders) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        List<PaymentMethod> paymentsList = mapper.readValue(file, new TypeReference<>() {});
        Map<String, PaymentMethod> payments = new HashMap<>(paymentsList.stream().collect(Collectors.toMap(PaymentMethod::getId, n -> n)));

        for (Order order : orders) {
            for (String s : order.getPromotions()) {
                payments.get(s).incrementOrdersAmount();
            }
        }
        payments.get("PUNKTY").setOrdersAmount(orders.size());

        return payments;
    }
}
