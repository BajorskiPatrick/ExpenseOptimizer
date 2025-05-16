package ocado.utils;

import ocado.model.Order;
import ocado.model.PaymentMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Utility class providing necessary methods for optimization.
 */
public class OptimizerUtils {
    /**
     * Default constructor creating an instance of the class.
     */
    public OptimizerUtils() {}

    /**
     * Processes payment for an order using the specified payment method.
     *
     * @param payment the payment method
     * @param order the order to be paid
     * @throws IllegalArgumentException exception thrown from {@link PaymentMethod#spend(BigDecimal)} and propagated further
     */
    public void pay(PaymentMethod payment, Order order) throws IllegalArgumentException {
        BigDecimal multiplier = new BigDecimal("1.00").subtract(payment.getDiscount().divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP));
        payment.spend(order.getValue().multiply(multiplier).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Processes payment for a specified amount using the chosen payment method.
     *
     * @param payment the payment method
     * @param value the amount to be paid
     * @throws IllegalArgumentException exception thrown from {@link PaymentMethod#spend(BigDecimal)} and propagated further
     */
    public void pay(PaymentMethod payment, BigDecimal value) throws IllegalArgumentException {
        payment.spend(value.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Finds the best card to pay the remaining balance of an order.<br>
     * Uses heuristics (in order):
     * <ul>
     *   <li>'Method not present in the promotions list of any unprocessed order'. In case of a tie:
     *      <ul>
     *          <li>'Minimum remaining limit', further ties are not resolved</li>
     *      </ul>
     *   </li>
     *   <li>'Minimum discount'. In case of a tie:
     *      <ul>
     *          <li>'Minimum number of unprocessed orders where the payment method is listed in promotions'</li>
     *          <li>'Minimum remaining limit', further ties are not resolved</li>
     *      </ul>
     *   </li>
     * </ul>
     *
     * @param methods the list of available {@link PaymentMethod}
     * @return the best {@link PaymentMethod} or null if not found
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
            // We are returning this unique card, which cannot be used as a promotion anywhere else.
            return zeroOrdersMethods.getFirst();
        }
        else if (zeroOrdersMethods.size() > 1) {
            // We return the method with 0 orders amount, which also has a minimum limit.
            return findWithMinLimit(zeroOrdersMethods).getFirst();
        }

        List<PaymentMethod> lowestDiscountMethods = findLowestDiscountMethods(methods);
        if (lowestDiscountMethods.size() == 1) {
            // We return the one with the smallest possible discount.
            return lowestDiscountMethods.getFirst();
        }
        else {
            // We have a tie. We are looking for the most optimal method (using the CARD-CARD tie rules).
            return findOptimalCard(lowestDiscountMethods);
        }
    }

    /**
     * Finds the optimal card from a list of payment methods.<br>
     * Uses heuristics (in order, until a 'winner' is determined):
     * <ul>
     *   <li>'Minimum number of unprocessed orders where the payment method is listed in promotions'</li>
     *   <li>'Minimum remaining limit', further ties are not resolved</li>
     * </ul>
     *
     * @param bestMethods the list of {@link PaymentMethod} to choose from
     * @return the optimal {@link PaymentMethod}
     */
    public PaymentMethod findOptimalCard(List<PaymentMethod> bestMethods) {
        int minOrdersAmount = bestMethods.stream().mapToInt(PaymentMethod::getOrdersAmount).min().orElse(-1);
        List<PaymentMethod> methodsWithMinOrdersAmount = bestMethods.stream().filter(pm -> pm.getOrdersAmount() == minOrdersAmount).toList();

        if (methodsWithMinOrdersAmount.size() == 1) {
            // We choose the one with the least amount of unprocessed orders that can bring a promotion
            // (is present on the promotions list).
            return methodsWithMinOrdersAmount.getFirst();
        }
        else {
            // We choose the one with the smallest limit.
            return findWithMinLimit(methodsWithMinOrdersAmount).getFirst();
        }
    }

    /**
     * Finds payment methods with the lowest discount.
     *
     * @param methods the list of {@link PaymentMethod} to check
     * @return a list of {@link PaymentMethod} with the lowest discount
     */
    public List<PaymentMethod> findLowestDiscountMethods(List<PaymentMethod> methods) {
        // We return methods that have a minimal discount.
        BigDecimal minDiscount = methods.stream().map(PaymentMethod::getDiscount).min(BigDecimal::compareTo).orElse(null);
        return methods.stream().filter(pm -> pm.getDiscount().compareTo(minDiscount) == 0).toList();
    }

    /**
     * Finds payment methods with the minimum limit.
     *
     * @param toCheck the list of payment methods to check
     * @return a list of {@link PaymentMethod} with the minimum limit
     */
    public List<PaymentMethod> findWithMinLimit(List<PaymentMethod> toCheck) {
        // We return methods that have a minimum limit.
        BigDecimal minLimit = toCheck.stream().map(PaymentMethod::getLimit).min(BigDecimal::compareTo).orElse(null);
        return toCheck.stream().filter(pm -> pm.getLimit().compareTo(minLimit) == 0).toList();
    }

    /**
     * Finds payment methods with the maximum limit.
     *
     * @param toCheck the list of {@link PaymentMethod} to check
     * @return a list of {@link PaymentMethod} with the maximum limit
     */
    public List<PaymentMethod> findWithMaxLimit(List<PaymentMethod> toCheck) {
        // We return methods that have a maximum limit.
        BigDecimal maxLimit = toCheck.stream().map(PaymentMethod::getLimit).max(BigDecimal::compareTo).orElse(null);
        return toCheck.stream().filter(pm -> pm.getLimit().compareTo(maxLimit) == 0).toList();
    }
}
