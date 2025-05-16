package ocado.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Klasa reprezentująca zamówienie.
 */
@Getter
@Setter
public class Order {
    /** Unikalny identyfikator zamówienia */
    private String id;

    /** Wartość zamówienia */
    private BigDecimal value;

    /** Lista identyfikatorów {@link PaymentMethod} dostępnych jako promocje dla zamówienia */
    private final List<String> promotions = new ArrayList<>();

    /** Mapa {@link PaymentMethod} wykorzystanych do opłacenia tego zamówienia, wraz z kwotami */
    private final Map<String, BigDecimal> usedPaymentsMethods = new HashMap<>();

    /**
     * Konstruktor używany przez Jackson do utworzenia obiekty z danych przekazanych w pliku JSON
     *
     * @param id identyfikator zamówienia
     * @param value wartość zamówienia, jako string z zaokrągleniem do 2 miejsc po przecinku
     * @param promotions tablica string będąca listą identyfikatorów promocyjnych {@link PaymentMethod}
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
     * Dodaje informację o wykorzystanej metodzie płatności dla zamówienia.
     *
     * @param method identyfikator {@link PaymentMethod}
     * @param value kwota płatności
     */
    public void usePaymentsMethod(String method, BigDecimal value) {
        usedPaymentsMethods.put(method, value.setScale(2, RoundingMode.HALF_UP));
    }
}
