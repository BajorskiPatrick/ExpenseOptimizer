package ocado.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Klasa reprezentująca metodę płatności.
 */
@Getter
@Setter
public class PaymentMethod {
    /** Unikalny identyfikator metody płatności */
    private String id;

    /** Wartość rabatu w procentach */
    private int discount;

    /** Dostępny limit płatności */
    private double limit;

    /** Liczba nieprzetworzonych jeszcze zamówień, dla których metoda może być użyta jako promocja */
    private int ordersAmount;

    /** Łączna kwota wydana tą metodą płatności */
    private double spending;

    /**
     * Domyślny konstruktor tworzący obiekt klasy.
     */
    public PaymentMethod() {}

    /**
     * Konstruktor używany do utworzenia "sztucznej" metody płatności, gdy metoda PUNKTY nie została zdefiniowana.
     * @param discount zniżka dla tworzonej metody
     * @param limit limit dla tworzonej metody
     */
    public PaymentMethod(int discount, double limit) {
        this.discount = discount;
        this.limit = limit;
    }

    /**
     * Zwiększa licznik zamówień, dla których metoda może być użyta jako promocja.
     */
    public void incrementOrdersAmount() {
        ordersAmount++;
    }

    /**
     * Zmniejsza licznik zamówień, dla których metoda może być użyta jako promocja.
     */
    public void decrementOrdersAmount() {
        if (ordersAmount > 0) {
            ordersAmount--;
        }
    }

    /**
     * Realizuje płatność określoną kwotą tą metodą płatności.
     *
     * @param amount kwota do zapłacenia
     * @throws IllegalArgumentException rzucany, gdy kwota do wydania przekracza limit metody
     */
    public void spend(double amount) throws IllegalArgumentException {
        if (amount < 0) {
            throw new IllegalArgumentException("Kwota do wydania nie moze byc liczba ujemna!");
        }
        if (amount > limit) {
            throw new IllegalArgumentException("Kwota do wydania przekracza limit metody!");
        }
        spending += amount;
        limit -= amount;
    }

    /**
     * Zwraca do dostępnego limitu podaną kwotę płatności (cofa transakcję).
     *
     * @param amount kwota do zwrotu
     * @throws IllegalArgumentException rzucany, gdy kwota do oddania przewyższa faktyczne wydatki
     */
    public void getMoneyBack(double amount) throws IllegalArgumentException {
        if (amount < 0) {
            throw new IllegalArgumentException("Kwota do oddania nie moze byc liczba ujemna!");
        }
        if (amount > spending) {
            throw new IllegalArgumentException("Kwota do oddania przewyzsza faktyczne wydatki!");
        }
        spending -= amount;
        limit += amount;
    }

    /**
     * Zwraca informację o wydatkach w formacie tekstowym.
     *
     * @return string w formacie "id wydana_kwota"
     */
    public String printSpending() {
        return id + " " + spending;
    }
}
