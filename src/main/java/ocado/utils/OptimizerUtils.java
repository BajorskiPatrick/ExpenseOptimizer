package ocado.utils;

import ocado.model.Order;
import ocado.model.PaymentMethod;

import java.util.List;

/**
 * Klasa narzędziowa dostarczająca niezbędnych metod do przeprowadzenia optymalizacji.
 */
public class OptimizerUtils {
    /**
     * Domyślny konstruktor tworzący obiekt klasy
     */
    public OptimizerUtils() {}

    /**
     * Metoda realizująca płatność za zamówienie wybraną metodą płatności.
     *
     * @param payment metoda płatności
     * @param order zamówienie do opłacenia
     * @throws IllegalArgumentException wyjątek rzucany z {@link PaymentMethod#spend(double)} i przekazywany dalej
     */
    public void pay(PaymentMethod payment, Order order) throws IllegalArgumentException {
        payment.spend(order.getValue() * (1 - (double) payment.getDiscount() / 100));
    }

    /**
     * Metoda realizująca płatność na określoną kwotę poprzez wybraną metodą płatności.
     *
     * @param payment metoda płatności
     * @param value kwota do zapłacenia
     * @throws IllegalArgumentException wyjątek rzucany z {@link PaymentMethod#spend(double)} i przekazywany dalej
     */
    public void pay(PaymentMethod payment, double value) throws IllegalArgumentException {
        payment.spend(value);
    }

    /**
     * Znajduje najlepszą kartę do opłacenia reszty zamówienia.<br>
     * Korzysta z heurystyk (w kolejności):
     * <ul>
     *   <li>'Metoda, której nie ma na liście promotions żadnego nieprzetworzonego jeszcze zamówienia'. W przypadku remisu:
     *      <ul>
     *          <li>'Minimalny pozostały limit', dalsze remisy nie są rozpatrywane</li>
     *      </ul>
     *   </li>
     *   <li>'Minimalna zniżka'. W przypadku remisu:
     *      <ul>
     *          <li>'Minimalna liczba nieprzetworzonych zamówień, na których liście promotions znajduje się metoda płatności'</li>
     *          <li>'Minimalny pozostały limit', dalsze remisy nie są rozpatrywane</li>
     *      </ul>
     *   </li>
     * </ul>
     *
     * @param methods lista dostępnych metod płatności
     * @return najlepsza metoda płatności lub null, jeśli nie znaleziono
     */
    public PaymentMethod findBestCardToPayRest(List<PaymentMethod> methods) {
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() == 1) {
            return methods.getFirst();
        }

        List<PaymentMethod> zeroOrdersMethods = methods.stream().
                filter(pm -> pm.getOrdersAmount() == 0).toList();

        if (zeroOrdersMethods.size() == 1) {
            // Zwracamy tę jedyną kartę, która nie może być stosowana jako promocja nigdzie indziej.
            return zeroOrdersMethods.getFirst();
        }
        else if (zeroOrdersMethods.size() > 1) {
            // Zwracamy tą z metod mających 0 orders amount, która dodatkowo ma minimalny limit.
            return findWithMinLimit(zeroOrdersMethods).getFirst();
        }

        List<PaymentMethod> lowestDiscountMethods = findLowestDiscountMethods(methods);
        if (lowestDiscountMethods.size() == 1) {
            // Zwracamy tę o najmniejszej możliwej zniżce.
            return lowestDiscountMethods.getFirst();
        }
        else {
            // Mamy remis. Szukamy najoptymalniejszej z metod (stosując reguły remisu KARTA-KARTA).
            return findOptimalCard(lowestDiscountMethods);
        }
    }

    /**
     * Znajduje optymalną kartę spośród listy metod płatności.<br>
     * Korzysta z heurystyk (w kolejności, do momentu wyłonienia 'zwycięzcy'):
     * <ul>
     *   <li>'Minimalna liczba nieprzetworzonych zamówień, na których liście promotions znajduje się metoda płatności'</li>
     *   <li>'Minimalny pozostały limit', dalsze remisy nie są rozpatrywane</li>
     * </ul>
     *
     * @param bestMethods lista metod płatności do wyboru
     * @return optymalna metoda płatności
     */
    public PaymentMethod findOptimalCard(List<PaymentMethod> bestMethods) {
        int minOrdersAmount = bestMethods.stream().mapToInt(PaymentMethod::getOrdersAmount).min().orElse(-1);
        List<PaymentMethod> methodsWithMinOrdersAmount = bestMethods.stream().filter(pm -> pm.getOrdersAmount() == minOrdersAmount).toList();

        if (methodsWithMinOrdersAmount.size() == 1) {
            // Wybieramy tę z najmniejszą ilością nieprzetworzonych zamówień, dla których może przynieść promocję
            // (jest obecna na liście promotions).
            return methodsWithMinOrdersAmount.getFirst();
        }
        else {
            // Wybieramy tę z najmniejszym limitem.
            return findWithMinLimit(methodsWithMinOrdersAmount).getFirst();
        }
    }

    /**
     * Znajduje metody płatności z najniższym rabatem.
     *
     * @param methods lista metod płatności do sprawdzenia
     * @return lista metod z najniższym rabatem
     */
    public List<PaymentMethod> findLowestDiscountMethods(List<PaymentMethod> methods) {
        // Zwracamy metody, które mają minimalny rabat.
        int minDiscount = methods.stream().mapToInt(PaymentMethod::getDiscount).min().orElse(-1);
        return methods.stream().filter(pm -> pm.getDiscount() == minDiscount).toList();
    }

    /**
     * Znajduje metody płatności z minimalnym limitem.
     *
     * @param toCheck lista metod płatności do sprawdzenia
     * @return lista metod z minimalnym limitem
     */
    public List<PaymentMethod> findWithMinLimit(List<PaymentMethod> toCheck) {
        // Zwracamy metody, które mają minimalny limit.
        double minLimit = toCheck.stream().mapToDouble(PaymentMethod::getLimit).min().orElse(-1.0);
        return toCheck.stream().filter(pm -> pm.getLimit() == minLimit).toList();
    }

    /**
     * Znajduje metody płatności z maksymalnym limitem.
     *
     * @param toCheck lista metod płatności do sprawdzenia
     * @return lista metod z maksymalnym limitem
     */
    public List<PaymentMethod> findWithMaxLimit(List<PaymentMethod> toCheck) {
        // Zwracamy metody, które mają maksymalny limit.
        double maxLimit = toCheck.stream().mapToDouble(PaymentMethod::getLimit).max().orElse(-1.0);
        return toCheck.stream().filter(pm -> pm.getLimit() == maxLimit).toList();
    }
}
