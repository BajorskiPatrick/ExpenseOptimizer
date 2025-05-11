package ocado.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Klasa reprezentująca zamówienie.
 */
@Getter
@Setter
public class Order {
    /** Unikalny identyfikator zamówienia */
    private String id;

    /** Wartość zamówienia */
    private double value;

    /** Lista promocji dostępnych dla zamówienia */
    private final List<String> promotions = new ArrayList<>();

    /** Mapa metod płatności wykorzystanych do opłacenia tego zamówienia, wraz z kwotami */
    private final Map<String, Double> usedPaymentsMethods = new HashMap<>();

    /**
     * Domyślny konstruktor tworzący obiekt klasy.
     */
    public Order() {}

    /**
     * Dodaje informację o wykorzystanej metodzie płatności dla zamówienia.
     *
     * @param method identyfikator metody płatności
     * @param value kwota płatności
     */
    public void usePaymentsMethod(String method, double value) {
        usedPaymentsMethods.put(method, value);
    }
}
