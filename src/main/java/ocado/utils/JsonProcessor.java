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

/**
 * Utility class for processing JSON files into domain objects.
 */
public class JsonProcessor {
    /**
     * Default constructor creating an instance of the class.
     */
    public JsonProcessor() {}

    /**
     * Converts a JSON file containing orders into a list of Order objects.
     *
     * @param file the JSON file containing orders
     * @return a list of Order objects
     * @throws IOException if a file read error occurs
     * @throws FileNotFoundException if the file does not exist
     */
    public static List<Order> getOrdersAsList(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }
        if (!file.getName().toLowerCase().endsWith(".json")) {
            throw new IllegalArgumentException("The specified file is not a file with the extension '.json': " + file.getAbsolutePath());
        }
        if (file.length() == 0) {
            return new ArrayList<>();
        }

        ObjectMapper mapper = new ObjectMapper();
        return new ArrayList<>(mapper.readValue(file, new TypeReference<>() {
        }));
    }

    /**
     * Converts a JSON file containing payment methods into a map of {@link PaymentMethod} objects.
     *
     * @param file the JSON file containing payment methods
     * @param orders the list of orders used to update the order count for payment methods
     * @return a map of {@link PaymentMethod} objects with keys being the method identifiers
     * @throws IOException if a file read error occurs
     * @throws FileNotFoundException if the file does not exist
     */
    public static Map<String, PaymentMethod> getPaymentsAsMap(File file, List<Order> orders) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }
        if (!file.getName().toLowerCase().endsWith(".json")) {
            throw new IllegalArgumentException("The specified file is not a file with the extension '.json': " + file.getAbsolutePath());
        }
        if (file.length() == 0) {
            return new HashMap<>();
        }

        ObjectMapper mapper = new ObjectMapper();
        List<PaymentMethod> paymentsList = mapper.readValue(file, new TypeReference<>() {});
        if (paymentsList.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, PaymentMethod> payments = new HashMap<>(paymentsList.stream().collect(Collectors.toMap(PaymentMethod::getId, n -> n)));

        for (Order order : orders) {
            for (String s : order.getPromotions()) {
                if (!payments.containsKey(s)) {
                    continue;
                }
                payments.get(s).incrementOrdersAmount();
            }
        }

        return payments;
    }
}
