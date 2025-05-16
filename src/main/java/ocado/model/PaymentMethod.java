package ocado.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Class representing a payment method.
 */
@Getter
@Setter
public class PaymentMethod {
    /** Unique payment method identifier */
    private String id;

    /** Discount value in percent */
    private BigDecimal discount;

    /** Available payment limit */
    private BigDecimal limit;

    /** Number of unprocessed orders where the method can be used as a promotion */
    private int ordersAmount;

    /** Total amount spent using this payment method */
    private BigDecimal spending = new BigDecimal("0.00").setScale(2, RoundingMode.HALF_UP);

    /**
     * Constructor used by Jackson to create an object from JSON data.
     *
     * @param id the payment method identifier
     * @param discount the discount value for this payment method
     * @param limit the spending limit for this payment method
     */
    @JsonCreator
    public PaymentMethod(@JsonProperty("id") String id, @JsonProperty("discount") String discount, @JsonProperty("limit") String limit) {
        this.id = id;
        this.discount = new BigDecimal(!discount.isEmpty() ? discount : "0.00").setScale(2, RoundingMode.HALF_UP);
        this.limit = new BigDecimal(!limit.isEmpty() ? limit : "0.00").setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Constructor used to create an "artificial" payment method when the PUNKTY method is not defined.
     *
     * @param discount the discount for the created method
     * @param limit the limit for the created method
     */
    public PaymentMethod(String discount, String limit) {
        this.discount = new BigDecimal(!discount.isEmpty() ? discount : "0.00").setScale(2, RoundingMode.HALF_UP);
        this.limit = new BigDecimal(!limit.isEmpty() ? limit : "0.00").setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Increments the order count for which the method can be used as a promotion.
     */
    public void incrementOrdersAmount() {
        ordersAmount++;
    }

    /**
     * Decrements the order count for which the method can be used as a promotion.
     */
    public void decrementOrdersAmount() {
        if (ordersAmount > 0) {
            ordersAmount--;
        }
    }

    /**
     * Processes a payment of the specified amount using this payment method.
     *
     * @param amount the amount to be paid
     * @throws IllegalArgumentException thrown if the amount to be spent exceeds the method's limit
     */
    public void spend(BigDecimal amount) throws IllegalArgumentException {
        if (amount.compareTo(new BigDecimal("0.00")) < 0) {
            throw new IllegalArgumentException("The amount to be spent cannot be a negative number!");
        }
        if (amount.compareTo(limit) > 0) {
            throw new IllegalArgumentException("The amount to be spent exceeds the method limit!");
        }
        this.spending = this.spending.add(amount).setScale(2, RoundingMode.HALF_UP);
        this.limit =  this.limit.subtract(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the specified payment amount to the available limit (reverses a transaction).
     *
     * @param amount the amount to be returned
     * @throws IllegalArgumentException thrown if the amount to be returned exceeds the actual spending
     */
    public void getMoneyBack(BigDecimal amount) throws IllegalArgumentException {
        if (amount.compareTo(new BigDecimal("0.00")) < 0) {
            throw new IllegalArgumentException("The amount to be returned cannot be a negative number!");
        }
        if (amount.compareTo(spending) > 0) {
            throw new IllegalArgumentException("The amount to be returned exceeds the actual expenses!");
        }
        this.spending = this.spending.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        this.limit = this.limit.add(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns spending information as a formatted string.
     *
     * @return a string in the format "id spent_amount"
     */
    public String printSpending() {
        return id + " " + spending;
    }
}
