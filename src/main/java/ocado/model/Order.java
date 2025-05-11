package ocado.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class Order {
    private String id;
    private double value;
    private final List<String> promotions = new ArrayList<>();
    private final Map<String, Double> usedPaymentsMethods = new HashMap<>();

    public void usePaymentsMethod(String method, double value) {
        usedPaymentsMethods.put(method, value);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", value=" + value +
                ", promotions=" + promotions +
                '}';
    }
}
