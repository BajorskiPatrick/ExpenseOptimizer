package ocado.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Class representing an order.
 */
@Getter
@Setter
public class Order {
    /** Unique order identifier */
    private String id;

    /** Order value */
    private BigDecimal value;

    /** List of {@link PaymentMethod} identifiers available as promotions for the order */
    private final List<String> promotions = new ArrayList<>();

    /** Map of {@link PaymentMethod} used to pay for this order, along with amounts */
    private final Map<String, BigDecimal> usedPaymentsMethods = new HashMap<>();

    /**
     * Constructor used by Jackson to create an object from JSON data.
     *
     * @param id the order identifier
     * @param value the order value as a string rounded to 2 decimal places
     * @param promotions an array of strings representing the list of promotional {@link PaymentMethod} identifiers
     */
    @JsonCreator
    public Order(@JsonProperty("id") String id, @JsonProperty("value") String value, @JsonProperty("promotions") List<String> promotions) {
        this.id = id != null ? id : "";
        this.value = new BigDecimal(!value.isEmpty() ? value : "0.00").setScale(2, RoundingMode.HALF_UP);
        if (promotions != null) {
            this.promotions.addAll(promotions);
        }
    }

    /**
     * Adds information about the payment method used for the order.
     *
     * @param method the {@link PaymentMethod} identifier
     * @param value the payment amount
     */
    public void usePaymentsMethod(String method, BigDecimal value) {
        usedPaymentsMethods.put(method, value.setScale(2, RoundingMode.HALF_UP));
    }
}
