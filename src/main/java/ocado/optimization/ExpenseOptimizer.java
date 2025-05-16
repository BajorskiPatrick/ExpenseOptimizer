package ocado.optimization;

import ocado.model.Order;
import ocado.model.PaymentMethod;
import ocado.utils.OptimizerUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Klasa odpowiedzialna za optymalizację wydatków poprzez wybór najlepszych metod płatności dla zamówień.
 */
public class ExpenseOptimizer {
    /** Lista zamówień do optymalizacji */
    private final List<Order> orders;

    /** Mapa dostępnych metod płatności (z wyjątkiem metody punktowej) */
    private final Map<String, PaymentMethod> payments;

    /** Metoda płatności punktowej (PUNKTY) */
    private final PaymentMethod pointsMethod;

    /** Obiekt {@link OptimizerUtils} dostarczający niezbędnych metod */
    private final OptimizerUtils utils = new OptimizerUtils();

    /**
     * Konstruktor klasy ExpenseOptimizer.
     *
     * @param orders lista zamówień
     * @param payments mapa {@link PaymentMethod}
     * @param pointsMethod metoda płatności punktowej
     */
    public ExpenseOptimizer(List<Order> orders, Map<String, PaymentMethod> payments, PaymentMethod pointsMethod) {
        this.orders = orders;
        this.payments = payments;
        this.pointsMethod = pointsMethod;
    }

    /**
     * Główna metoda optymalizująca płatności dla zamówień.
     *
     * @return true, jeśli optymalizacja zakończyła się sukcesem, false w przeciwnym wypadku
     * @throws IllegalArgumentException wyjątek rzucany z jednej z metod {@link OptimizerUtils#pay(PaymentMethod, Order)},
     * {@link OptimizerUtils#pay(PaymentMethod, BigDecimal)}, {@link #spentRemainingPoints()} i przekazywany dalej
     */
    public boolean optimize() throws IllegalArgumentException {
        orders.sort(Comparator.comparing(Order::getValue).reversed());

        boolean wasEverythingPaid = true;
        for (Order order : orders) {
            BigDecimal bestPromotion = new BigDecimal("0.00");
            List<PaymentMethod> bestMethods = new ArrayList<>();

            // Znajdujemy maksymalną procentową zniżkę z tych, które mogą nam dać metody z listy promotions.
            for (String s : order.getPromotions()) {
                if (!payments.containsKey(s)) {
                    continue;
                }
                PaymentMethod pm = payments.get(s);
                pm.decrementOrdersAmount();
                if (pm.getLimit().compareTo(order.getValue()) >= 0 && pm.getDiscount().compareTo(bestPromotion) > 0) {
                    bestPromotion = pm.getDiscount();
                }
            }

            // Jeśli płatność wszystkiego punktami jest możliwa, to wybieramy tę opcję. Jeśli nie, to rozpatrujemy
            // wszystkie metody, których zniżka równa najlepszej zniżce.
            if (pointsMethod.getLimit().compareTo(order.getValue()) >= 0
                    && pointsMethod.getDiscount().compareTo(bestPromotion) >= 0) {
                bestMethods.add(pointsMethod);
                bestPromotion = pointsMethod.getDiscount();
            }
            else if (bestPromotion.compareTo(new BigDecimal("0.00")) > 0) {
                for (String s : order.getPromotions()) {
                    if (!payments.containsKey(s)) {
                        continue;
                    }
                    if (payments.get(s).getDiscount().compareTo(bestPromotion) == 0) {
                        bestMethods.add(payments.get(s));
                    }
                }
            }

            if (bestPromotion.compareTo(new BigDecimal("10.00")) > 0) {
                if (bestMethods.size() == 1) {
                    // Brak remisu lub remis PUNKTY-KARTA.
                    utils.pay(bestMethods.getFirst(), order);
                    order.usePaymentsMethod(bestMethods.getFirst().getId(), order.getValue());
                }
                else {
                    // Remis KARTA-KARTA.
                    PaymentMethod pm = utils.findOptimalCard(bestMethods);
                    utils.pay(pm, order);
                    order.usePaymentsMethod(pm.getId(), order.getValue());
                }
            }
            else {
                BigDecimal toPayByPoints = order.getValue().multiply(new BigDecimal("0.10").setScale(2, RoundingMode.HALF_UP)); // Początkowo 10% zamówienia.
                BigDecimal toPayByCard = order.getValue().subtract(toPayByPoints.multiply(new BigDecimal("2.00")).setScale(2, RoundingMode.HALF_UP)).
                        setScale(2, RoundingMode.HALF_UP); // Bo 10% z punktów, plus drugie tyle rabatu.
                PaymentMethod pm;

                if (toPayByPoints.compareTo(pointsMethod.getLimit()) <= 0) {
                    // Stosujemy płatność PUNKTY_10 i chcemy opłacić z punktów tylko 10% (resztę punktów zachować na później).
                    // Resztę zamówienia chcemy opłacić "najgorszą" możliwą kartą.
                    BigDecimal finalToPayByCard = toPayByCard;

                    pm = utils.findBestCardToPayRest(payments.values().stream().filter(p -> p.getLimit().compareTo(finalToPayByCard) >= 0).toList());

                    if (pm == null) {
                        // Nie znaleźliśmy karty z limitem, który pozwoli opłacić punktami jedynie 10% - zatem szukamy
                        // karty z maksymalnym limitem <= niż aktualne "toPayByCard" i uzupełniamy punktami.
                        List<PaymentMethod> maxLimitCards = utils.findWithMaxLimit(payments.values().stream().toList());
                        pm = utils.findBestCardToPayRest(maxLimitCards);

                        if (pm != null && order.getValue().subtract(pm.getLimit()).compareTo(pointsMethod.getLimit()) <= 0) {
                            // Wystarcza punktów na uzupełnienie do wybranej karty, więc płacimy.
                            toPayByCard = pm.getLimit();
                            toPayByPoints = order.getValue().multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP).
                                    subtract(toPayByCard).setScale(2, RoundingMode.HALF_UP);
                        }
                        else {
                            // Doszliśmy do momentu, w którym nie jesteśmy w stanie dobrać żadnej metody płatności
                            // do zamówienia -> prawdopodobnie do tego przykładu zastosowana heurystyka się nie sprawdza,
                            // więc kończymy optymalizację :(.
                            wasEverythingPaid = false;
                            break;
                        }
                    }
                    utils.pay(pointsMethod, toPayByPoints);
                    utils.pay(pm, toPayByCard);
                    order.usePaymentsMethod("PUNKTY_10", toPayByPoints);
                    order.usePaymentsMethod(pm.getId(), toPayByCard);
                }
                else if (bestMethods.size() == 1) {
                    // Nie damy rady zapłacić punktami i jest brak remisu.
                    utils.pay(bestMethods.getFirst(), order);
                    order.usePaymentsMethod(bestMethods.getFirst().getId(), order.getValue());
                }
                else if (bestMethods.size() > 1) {
                    // Nie damy rady zapłacić punktami i jest remis KARTA-KARTA.
                    pm = utils.findOptimalCard(bestMethods);
                    utils.pay(pm, order);
                    order.usePaymentsMethod(pm.getId(), order.getValue());
                }
                else {
                    // Nie ma żadnej promocyjnej opcji płatności i PUNKTY_10 również nie mogą być zastosowane.
                    // Szukamy karty do opłacenia reszty (całego zamówienia) i ją stosujemy.
                    pm = utils.findBestCardToPayRest(payments.values().stream().filter(p -> p.getLimit().compareTo(order.getValue()) >= 0).toList());
                    if (pm == null) {
                        // Również dochodzimy do momentu, w którym nie jesteśmy w stanie dobrać żadnej metody płatności
                        // do zamówienia, więc kończymy optymalizację :(.
                        wasEverythingPaid = false;
                        break;
                    }
                    utils.pay(pm, order);
                    order.usePaymentsMethod(pm.getId(), order.getValue());
                }
            }
        }

        // Skończyliśmy główną pętlę algorytmu. Jeśli nie udało się w niej opłacić wszystkich zamówień lub wyczerpaliśmy
        // punkty -> po prostu zwracamy wynik działania algorytmu.
        if (!wasEverythingPaid || pointsMethod.getLimit().compareTo(new BigDecimal("0.00")) == 0) {
            return wasEverythingPaid;
        }

        // W innym wypadku będziemy iterować od ostatniego zamówienia i "poprawiać" wynik algorytmu, poprzez wydanie
        // pozostałych punktów na zamówienia opłacone metodą PUNKTY_10 -> będziemy "oddawać" określoną kwotę na kartę
        // użytą do uzupełnienia tego zamówienia i "dopłacać" tę kwotę punktami, aż do momentu, gdy pozbędziemy się
        // wszystkich punktów -> w duchu ograniczenia minimalizacji płatności kartą.
        spentRemainingPoints();

        return true;
    }

    /**
     * Metoda wydająca pozostałe punkty na zamówienia opłacone metodą PUNKTY_10.
     * @throws IllegalArgumentException wyjątek rzucany z {@link PaymentMethod#getMoneyBack(BigDecimal)} i przekazywany dalej
     */
    private void spentRemainingPoints() throws IllegalArgumentException {
        for (Order order : orders.reversed()) {
            // Zamówienie może mieć maksymalnie 2 opcje płatności -> PUNKTY_10 + KARTA.
            if (order.getUsedPaymentsMethods().size() != 2) {
                continue;
            }
            String usedCard = "";
            BigDecimal spentAmount = new BigDecimal("0.00").setScale(2, RoundingMode.HALF_UP);
            for (Map.Entry<String, BigDecimal> entry : order.getUsedPaymentsMethods().entrySet()) {
                if (!entry.getKey().equals("PUNKTY_10")) {
                    usedCard = entry.getKey();
                    spentAmount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
                    break;
                }
            }

            if (spentAmount.compareTo(pointsMethod.getLimit()) >= 0) {
                // W tej sytuacji wydaliśmy już wszystkie punkty, więc kończymy.
                payments.get(usedCard).getMoneyBack(pointsMethod.getLimit());
                pointsMethod.spend(pointsMethod.getLimit());
                break;
            }
            else {
                // Tutaj wydaliśmy tylko część naszych pozostałych punktów, więc idziemy do kolejnego zamówienia.
                payments.get(usedCard).getMoneyBack(spentAmount);
                pointsMethod.spend(spentAmount);
            }
        }
    }
}
