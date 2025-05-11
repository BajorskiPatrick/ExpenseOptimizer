package ocado.optimization;

import ocado.model.Order;
import ocado.model.PaymentMethod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ExpenseOptimizer {
    private final List<Order> orders;
    private final Map<String, PaymentMethod> payments;
    private final PaymentMethod pointsMethod;

    public ExpenseOptimizer(List<Order> orders, Map<String, PaymentMethod> payments, PaymentMethod pointsMethod) {
        this.orders = orders;
        this.payments = payments;
        this.pointsMethod = pointsMethod;
    }

    public boolean optimize() {
        orders.sort(Comparator.comparing(Order::getValue).reversed());

        boolean wasEverythingPaid = true;
        for (Order order : orders) {
            double bestPromotion = 0;
            List<PaymentMethod> bestMethods = new ArrayList<>();
            for (String s : order.getPromotions()) {
                PaymentMethod pm = payments.get(s);
                pm.setOrdersAmount(pm.getOrdersAmount() - 1);
                if (pm.getLimit() >= order.getValue() && pm.getDiscount() > bestPromotion) {
                    bestPromotion = pm.getDiscount();
                }
            }

            if (pointsMethod.getLimit() >= order.getValue()
                    && pointsMethod.getDiscount() >= bestPromotion) {
                bestMethods.add(pointsMethod);
                bestPromotion = pointsMethod.getDiscount();
            }
            else if (bestPromotion > 0) {
                for (String s : order.getPromotions()) {
                    if (payments.get(s).getDiscount() == bestPromotion) {
                        bestMethods.add(payments.get(s));
                    }
                }
            }

            if (bestPromotion > 10) {
                if (bestMethods.size() == 1) {
                    // Brak remisu lub remis PUNKTY-KARTA
                    pay(bestMethods.getFirst(), order);
                    order.usePaymentsMethod(bestMethods.getFirst().getId(), order.getValue());
                }
                else {
                    // Remis KARTA-KARTA
                    PaymentMethod pm = findOptimalCard(bestMethods);
                    pay(pm, order);
                    order.usePaymentsMethod(pm.getId(), order.getValue());
                }
            }
            else {
                double toPayByPoints = order.getValue() * 0.1; // Początkowo 10% zamówienia
                double toPayByCard = order.getValue() - toPayByPoints * 2; // Bo 10% z punktów plus drugie tyle rabatu
                PaymentMethod pm;
                if (toPayByPoints <= pointsMethod.getLimit()) {
                    // Stosujemy płatność PUNKTY_10 i chcemy opłacić z punktów tylko 10% (resztę zachować na później).
                    // Resztę zamówienia chcemy opłacić "najgorszą" możliwą kartą
                    double finalToPayByCard = toPayByCard;
                    pm = findBestCardToPayRest(payments.values().stream().filter(p -> p.getLimit() >= finalToPayByCard).toList());
                    if (pm == null) {
                        // Nie znaleźliśmy karty z limitem, który pozwoli opłacić punktami jedynie 10% - zatem szukamy
                        // karty z maksymalnym limitem <= niż aktualne "toPayByCard" i uzupełniamy punktami
                        List<PaymentMethod> maxLimitCards = findWithMaxLimit(payments.values().stream().toList());
                        pm = findBestCardToPayRest(maxLimitCards);
                        if (pm != null && order.getValue() - pm.getLimit() <= pointsMethod.getLimit()) {
                            // Wystarcza punktów na uzupełnienie do karty, więc płacimy
                            toPayByCard = pm.getLimit();
                            toPayByPoints = order.getValue() * 0.9 - toPayByCard;
                        }
                        else {
                            // Doszliśmy do momentu, w którym nie jesteśmy w stanie dobrać żadnej metody płatności
                            // do zamówienia -> prawdopodobnie do tego przykładu zastosowana heurystyka się nie sprawdza,
                            // więc kończymy optymalizację bez żadnego wyniku :(
                            wasEverythingPaid = false;
                            break;
                        }
                    }
                    pay(pointsMethod, toPayByPoints);
                    pay(pm, toPayByCard);
                    order.usePaymentsMethod("PUNKTY_10", toPayByPoints);
                    order.usePaymentsMethod(pm.getId(), toPayByCard);
                }
                else if (bestMethods.size() == 1) {
                    // Brak remisu przy bestPromotion <= 10
                    pay(bestMethods.getFirst(), order);
                    order.usePaymentsMethod(bestMethods.getFirst().getId(), order.getValue());
                }
                else if (bestMethods.size() > 1) {
                    // Remis KARTA-KARTA
                    pm = findOptimalCard(bestMethods);
                    pay(pm, order);
                    order.usePaymentsMethod(pm.getId(), order.getValue());
                }
                else {
                    // Nie ma żadnej promocyjnej opcji płatności i PUNKTY_10 również nie mogą być zastosowane.
                    // Szukamy karty do opłacenia reszty (całego zamówienia) i ją stosujemy
                    pm = findBestCardToPayRest(payments.values().stream().filter(p -> p.getLimit() >= order.getValue()).toList());
                    if (pm == null) {
                        wasEverythingPaid = false;
                        break;
                    }
                    pay(pm, order);
                    order.usePaymentsMethod(pm.getId(), order.getValue());
                }
            }
        }

        // Skończyliśmy główną pętlę algorytmu. Jeśli nie udało się w niej opłacić wszystkich zamówień lub wyczerpaliśmy
        // punkty -> po prostu zwracamy wynik działania algorytmu
        if (!wasEverythingPaid || pointsMethod.getLimit() == 0) {
            return wasEverythingPaid;
        }

        // W innym wypadku będziemy iterować od ostatniego zamówienia i "poprawiać" wynik algorytmu, poprzez wydanie
        // pozostałych punktów na zamówienia opłacone metodą PUNKTY_10 -> będziemy "oddawać" określoną kwotę na kartę
        // użytą do uzupełnienia tego zamówienia i "dopłacać" tę kwotę punktami, aż do momentu, gdy pozbędziemy się
        // wszystkich punktów -> w duchu ograniczenia "minimalizujemy płatności kartą"
        spentRemainingPoints();

        return true;
    }

    private void spentRemainingPoints() {
        for (Order order : orders.reversed()) {
            // Zamówienie może mieć maksymalnie 2 opcje płatności, wtedy -> PUNKTY_10 + KARTA
            if (order.getUsedPaymentsMethods().size() != 2) {
                continue;
            }
            String usedCard = "";
            double spentAmount = 0;
            for (Map.Entry<String, Double> entry : order.getUsedPaymentsMethods().entrySet()) {
                if (!entry.getKey().equals("PUNKTY_10")) {
                    usedCard = entry.getKey();
                    spentAmount = entry.getValue();
                    break;
                }
            }

            if (spentAmount >= pointsMethod.getLimit()) {
                // W tej sytuacji wydaliśmy już wszystkie punkty, więc kończymy
                payments.get(usedCard).getMoneyBack(pointsMethod.getLimit());
                pointsMethod.spend(pointsMethod.getLimit());
                break;
            }
            else {
                // Tutaj wydaliśmy tylko część naszych pozostałych punktów, więc idziemy do kolejnego zamówienia
                payments.get(usedCard).getMoneyBack(spentAmount);
                pointsMethod.spend(spentAmount);
            }
        }
    }

    private void pay(PaymentMethod payment, Order order) {
        payment.spend(order.getValue() * (1 - (double) payment.getDiscount() / 100));
    }

    private void pay(PaymentMethod payment, double value) {
        payment.spend(value);
    }

    private PaymentMethod findBestCardToPayRest(List<PaymentMethod> methods) {
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() == 1) {
            return methods.getFirst();
        }

        List<PaymentMethod> zeroOrdersMethods = methods.stream().
                filter(pm -> pm.getOrdersAmount() == 0).toList();

        if (zeroOrdersMethods.size() == 1) {
            // Zwracamy tę jedyną kartę, która nie może być stosowana jako promocja nigdzie indziej
            return zeroOrdersMethods.getFirst();
        }
        else if (zeroOrdersMethods.size() > 1) {
            // Zwracamy tą z metod mających 0 orders amount, która dodatkowo ma minimalny limit
            return findWithMinLimit(zeroOrdersMethods).getFirst();
        }

        List<PaymentMethod> lowestDiscountMethods = findLowestDiscountMethods(methods);
        if (lowestDiscountMethods.size() == 1) {
            // Zwracamy tą o najmniejszej możliwej zniżce (marnujemy najmniej)
            return lowestDiscountMethods.getFirst();
        }
        else {
            // Mamy remis. Szukamy najoptymalniejszej z metod (stosując reguły remisu KARTA-KARTA)
            return findOptimalCard(lowestDiscountMethods);
        }
    }

    private List<PaymentMethod> findLowestDiscountMethods(List<PaymentMethod> methods) {
        // Zwracamy metody, które mają minimalny rabat
        int minDiscount = methods.stream().mapToInt(PaymentMethod::getDiscount).min().orElse(-1);
        return methods.stream().filter(pm -> pm.getDiscount() == minDiscount).toList();
    }

    private PaymentMethod findOptimalCard(List<PaymentMethod> bestMethods) {
        int minOrdersAmount = bestMethods.stream().mapToInt(PaymentMethod::getOrdersAmount).min().orElse(-1);
        List<PaymentMethod> methodsWithMinOrdersAmount = bestMethods.stream().filter(pm -> pm.getOrdersAmount() == minOrdersAmount).toList();

        if (methodsWithMinOrdersAmount.size() == 1) {
            // Wybieramy tą z najmniejszą ilością nieopłaconych zamówień, dla których może przynieść promocję
            return methodsWithMinOrdersAmount.getFirst();
        }
        else {
            // Wybieramy tą z najmniejszym limitem
            return findWithMinLimit(methodsWithMinOrdersAmount).getFirst();
        }
    }

    private List<PaymentMethod> findWithMinLimit(List<PaymentMethod> toCheck) {
        double minLimit = toCheck.stream().mapToDouble(PaymentMethod::getLimit).min().orElse(-1.0);
        return toCheck.stream().filter(pm -> pm.getLimit() == minLimit).toList();
    }

    private List<PaymentMethod> findWithMaxLimit(List<PaymentMethod> toCheck) {
        double maxLimit = toCheck.stream().mapToDouble(PaymentMethod::getLimit).max().orElse(-1.0);
        return toCheck.stream().filter(pm -> pm.getLimit() == maxLimit).toList();
    }
}
