package ocado.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Klasa reprezentująca metodę płatności.
 */
@Getter
@Setter
public class PaymentMethod {
    /** Unikalny identyfikator metody płatności */
    private String id;

    /** Wartość rabatu w procentach */
    private BigDecimal discount;

    /** Dostępny limit płatności */
    private BigDecimal limit;

    /** Liczba nieprzetworzonych jeszcze zamówień, dla których metoda może być użyta jako promocja */
    private int ordersAmount;

    /** Łączna kwota wydana tą metodą płatności */
    private BigDecimal spending = new BigDecimal("0.00").setScale(2, RoundingMode.HALF_UP);

    /**
     * Konstruktor używany przez Jackson do utworzenia obiekty z danych przekazanych w pliku JSON
     * @param id identyfikator zamówienia
     * @param discount wartość zniżki dla tej metody płatności
     * @param limit wartość limitu wydatków dla tej metody płatności
     */
    @JsonCreator
    public PaymentMethod(@JsonProperty("id") String id, @JsonProperty("discount") String discount, @JsonProperty("limit") String limit) {
        this.id = id;
        this.discount = new BigDecimal(!discount.isEmpty() ? discount : "0.00").setScale(2, RoundingMode.HALF_UP);
        this.limit = new BigDecimal(!limit.isEmpty() ? limit : "0.00").setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Konstruktor używany do utworzenia "sztucznej" metody płatności, gdy metoda PUNKTY nie została zdefiniowana.
     * @param discount zniżka dla tworzonej metody
     * @param limit limit dla tworzonej metody
     */
    public PaymentMethod(String discount, String limit) {
        this.discount = new BigDecimal(!discount.isEmpty() ? discount : "0.00").setScale(2, RoundingMode.HALF_UP);
        this.limit = new BigDecimal(!limit.isEmpty() ? limit : "0.00").setScale(2, RoundingMode.HALF_UP);
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
    public void spend(BigDecimal amount) throws IllegalArgumentException {
        if (amount.compareTo(new BigDecimal("0.00")) < 0) {
            throw new IllegalArgumentException("Kwota do wydania nie moze byc liczba ujemna!");
        }
        if (amount.compareTo(limit) > 0) {
            throw new IllegalArgumentException("Kwota do wydania przekracza limit metody!");
        }
        this.spending = this.spending.add(amount).setScale(2, RoundingMode.HALF_UP);
        this.limit =  this.limit.subtract(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Zwraca do dostępnego limitu podaną kwotę płatności (cofa transakcję).
     *
     * @param amount kwota do zwrotu
     * @throws IllegalArgumentException rzucany, gdy kwota do oddania przewyższa faktyczne wydatki
     */
    public void getMoneyBack(BigDecimal amount) throws IllegalArgumentException {
        if (amount.compareTo(new BigDecimal("0.00")) < 0) {
            throw new IllegalArgumentException("Kwota do oddania nie moze byc liczba ujemna!");
        }
        if (amount.compareTo(spending) > 0) {
            throw new IllegalArgumentException("Kwota do oddania przewyzsza faktyczne wydatki!");
        }
        this.spending = this.spending.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        this.limit = this.limit.add(amount).setScale(2, RoundingMode.HALF_UP);
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
