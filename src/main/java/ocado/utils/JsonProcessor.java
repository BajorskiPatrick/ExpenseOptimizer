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
 * Klasa narzędziowa do przetwarzania plików JSON na obiekty domenowe.
 */
public class JsonProcessor {
    /**
     * Domyślny konstruktor tworzący obiekt klasy.
     */
    public JsonProcessor() {}

    /**
     * Konwertuje plik JSON z zamówieniami na listę obiektów Order.
     *
     * @param file plik JSON z zamówieniami
     * @return lista obiektów Order
     * @throws IOException jeśli wystąpi błąd odczytu pliku
     * @throws FileNotFoundException jeśli plik nie istnieje
     */
    public static List<Order> getOrdersAsList(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }
        if (!file.getName().toLowerCase().endsWith(".json")) {
            throw new IllegalArgumentException("Podany plik nie jest plikiem z rozszerzeniem '.json': " + file.getAbsolutePath());
        }
        if (file.length() == 0) {
            return new ArrayList<>();
        }

        ObjectMapper mapper = new ObjectMapper();
        return new ArrayList<>(mapper.readValue(file, new TypeReference<>() {
        }));
    }

    /**
     * Konwertuje plik JSON z metodami płatności na mapę obiektów {@link PaymentMethod}.
     *
     * @param file plik JSON z metodami płatności
     * @param orders lista zamówień używana do aktualizacji liczby zamówień dla metod płatności
     * @return mapa obiektów {@link PaymentMethod} z kluczami będącymi identyfikatorami metod
     * @throws IOException jeśli wystąpi błąd odczytu pliku
     * @throws FileNotFoundException jeśli plik nie istnieje
     */
    public static Map<String, PaymentMethod> getPaymentsAsMap(File file, List<Order> orders) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("Nie znaleziono pliku: " + file.getAbsolutePath());
        }
        if (!file.getName().toLowerCase().endsWith(".json")) {
            throw new IllegalArgumentException("Podany plik nie jest plikiem z rozszerzeniem '.json': " + file.getAbsolutePath());
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
