package ocado.optimization;

import ocado.model.Order;
import ocado.model.PaymentMethod;
import ocado.utils.OptimizerUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Class responsible for optimizing expenses by selecting the best payment methods for orders.
 */
public class ExpenseOptimizer {
    /** List of orders to optimize */
    private final List<Order> orders;

    /** Map of available payment methods (excluding the points method) */
    private final Map<String, PaymentMethod> payments;

    /** Points payment method (PUNKTY) */
    private final PaymentMethod pointsMethod;

    /** {@link OptimizerUtils} object providing necessary methods */
    private final OptimizerUtils utils = new OptimizerUtils();

    /**
     * Constructor for the ExpenseOptimizer class.
     *
     * @param orders the list of orders
     * @param payments the map of {@link PaymentMethod}
     * @param pointsMethod the points payment method
     */
    public ExpenseOptimizer(List<Order> orders, Map<String, PaymentMethod> payments, PaymentMethod pointsMethod) {
        this.orders = orders;
        this.payments = payments;
        this.pointsMethod = pointsMethod;
    }

    /**
     * Main method for optimizing payments for orders.
     *
     * @return true if optimization was successful, false otherwise
     * @throws IllegalArgumentException exception thrown from {@link OptimizerUtils#pay(PaymentMethod, Order)},
     * {@link OptimizerUtils#pay(PaymentMethod, BigDecimal)}, or {@link #spentRemainingPoints()} and propagated further
     */
    public boolean optimize() throws IllegalArgumentException {
        orders.sort(Comparator.comparing(Order::getValue).reversed());

        boolean wasEverythingPaid = true;
        for (Order order : orders) {
            BigDecimal bestPromotion = new BigDecimal("0.00");
            List<PaymentMethod> bestMethods = new ArrayList<>();

            // We find the maximum percentage discount that the methods from the promotions list can give us.
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

            // If it is possible to pay everything with points, we choose this option. If not, we consider
            // all methods whose discount equals the best discount.
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
                    // No tie or a POINTS-CARD tie.
                    utils.pay(bestMethods.getFirst(), order);
                    order.usePaymentsMethod(bestMethods.getFirst().getId(), order.getValue());
                }
                else {
                    // CARD-CARD tie.
                    PaymentMethod pm = utils.findOptimalCard(bestMethods);
                    utils.pay(pm, order);
                    order.usePaymentsMethod(pm.getId(), order.getValue());
                }
            }
            else {
                BigDecimal toPayByPoints = order.getValue().multiply(new BigDecimal("0.10").setScale(2, RoundingMode.HALF_UP)); // Initially 10% of the order.
                BigDecimal toPayByCard = order.getValue().subtract(toPayByPoints.multiply(new BigDecimal("2.00")).setScale(2, RoundingMode.HALF_UP)).
                        setScale(2, RoundingMode.HALF_UP); // Because 10% of the points, plus twice the discount.
                PaymentMethod pm;

                if (toPayByPoints.compareTo(pointsMethod.getLimit()) <= 0) {
                    // We use the POINTS_10 payment method, and we want to pay only 10% of the points (keep the rest of the points for later).
                    // We want to pay for the rest of the order with the "worst" card possible.
                    BigDecimal finalToPayByCard = toPayByCard;

                    pm = utils.findBestCardToPayRest(payments.values().stream().filter(p -> p.getLimit().compareTo(finalToPayByCard) >= 0).toList());

                    if (pm == null) {
                        // We didn't find a card with a limit that would allow you to pay with points only 10% -
                        // so we're looking for a card with a maximum limit <= than the current "toPayByCard" and filling it in with points.
                        List<PaymentMethod> maxLimitCards = utils.findWithMaxLimit(payments.values().stream().toList());
                        pm = utils.findBestCardToPayRest(maxLimitCards);

                        if (pm != null && order.getValue().subtract(pm.getLimit()).compareTo(pointsMethod.getLimit()) <= 0) {
                            // Wystarcza punktów na uzupełnienie do wybranej karty, więc płacimy.
                            toPayByCard = pm.getLimit();
                            toPayByPoints = order.getValue().multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP).
                                    subtract(toPayByCard).setScale(2, RoundingMode.HALF_UP);
                        }
                        else {
                            // We have reached the point where we are unable to select any payment method
                            // for the order -> probably used heuristics do not work for this example,
                            // so we end the optimization :
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
                    // We won't be able to pay with points, and it's a no draw.
                    utils.pay(bestMethods.getFirst(), order);
                    order.usePaymentsMethod(bestMethods.getFirst().getId(), order.getValue());
                }
                else if (bestMethods.size() > 1) {
                    // We won't be able to pay with points and it's a CARD-CARD tie.
                    pm = utils.findOptimalCard(bestMethods);
                    utils.pay(pm, order);
                    order.usePaymentsMethod(pm.getId(), order.getValue());
                }
                else {
                    // There is no promotional payment option and POINTS_10 cannot be applied either.
                    // We look for a card to pay for the rest (the entire order) and use it.
                    pm = utils.findBestCardToPayRest(payments.values().stream().filter(p -> p.getLimit().compareTo(order.getValue()) >= 0).toList());
                    if (pm == null) {
                        // We also reach the point where we are unable to select any payment method
                        // for the order, so we end the optimization :(.
                        wasEverythingPaid = false;
                        break;
                    }
                    utils.pay(pm, order);
                    order.usePaymentsMethod(pm.getId(), order.getValue());
                }
            }
        }

        // We have finished the main loop of the algorithm. If we have not managed to pay all orders in it,
        // or we have exhausted points -> we simply return the result of the algorithm.
        if (!wasEverythingPaid || pointsMethod.getLimit().compareTo(new BigDecimal("0.00")) == 0) {
            return wasEverythingPaid;
        }

        // Otherwise, we will iterate from the last order and "improve" the algorithm's result by spending
        // the remaining points on orders paid with the POINTS_10 method -> we will give back a certain amount to the card
        // used to complete this order and pay this amount with points until we get rid of
        // all points -> in the spirit of minimizing card payments.
        spentRemainingPoints();

        return true;
    }

    /**
     * Spends the remaining points on orders paid with the PUNKTY_10 method.
     * @throws IllegalArgumentException exception thrown from {@link PaymentMethod#getMoneyBack(BigDecimal)} and propagated further
     */
    private void spentRemainingPoints() throws IllegalArgumentException {
        for (Order order : orders.reversed()) {
            // An order can have a maximum of 2 payment options -> POINTS_10 + CARD.
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
                // At this point we've already spent all our points, so we're done.
                payments.get(usedCard).getMoneyBack(pointsMethod.getLimit());
                pointsMethod.spend(pointsMethod.getLimit());
                break;
            }
            else {
                // Here we have only spent some of our remaining points, so we move on to the next order.
                payments.get(usedCard).getMoneyBack(spentAmount);
                pointsMethod.spend(spentAmount);
            }
        }
    }
}
