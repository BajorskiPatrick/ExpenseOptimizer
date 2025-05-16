# ExpenseOptimizer

## Project Goal
The aim of this project was to develop an algorithm that, for a given list of orders, available promotions, 
and the customer's wallet (payment methods, limits, discounts), selects the optimal payment method for each order. 
The algorithm strives to maximize the total discount while satisfying all constraints (full payment of orders, 
not exceeding payment method limits). Additionally, the algorithm minimizes card payments, preferring to spend loyalty 
points if it does not reduce the due discount.

## Payment and Discount Rules

### Order Payment Options:
Each order can be paid:
1.  In full using a single traditional payment method (e.g., one card).
2.  In full using loyalty points.
3.  Partially with points and partially with a single traditional payment method.

### Discounts:
1.  **Bank Discounts:**
    *   Offered based on agreements with selected banks.
    *   Applied **only when** an order is paid **in full** with a specific bank card covered by the promotion.
    *   Each order has an assigned subset of promotions (payment methods) that can be applied to it.
    *   The system does not support bank discounts for partial card payments.
2.  **Loyalty Point Discounts:**
    *   **Full payment with points ("PUNKTY"):** If the entire order is paid with points, a percentage discount defined for the "PUNKTY" method is applied.
    *   **Partial payment with points ("PUNKTY\_10"):** If the customer pays **at least 10% of the order value** (before discount) with loyalty points, the store applies an additional **10% discount on the entire order**.
        *   Applying this discount excludes the possibility of applying an additional bank card discount for the remaining payment portion.
    *   Promotions for using points (PUNKTY and PUNKTY\_10) are not assigned to specific orders and can always be applied (if their respective conditions are met).

## Input Data Structure

The application accepts two JSON files as command-line arguments:

1.  **`orders.json`**: List of orders:
    ```json
    [
        {"id": "ORDER1", "value": "100.00", "promotions": ["mZysk"]},
        {"id": "ORDER2", "value": "200.00", "promotions": ["BosBankrut"]},
        {"id": "ORDER3", "value": "150.00", "promotions": ["mZysk", "BosBankrut"]},
        {"id": "ORDER4", "value": "50.00"}
    ]
    ```
    Where:
    *   `id`: Order identifier.
    *   `value`: Order amount with precision to two decimal places.
    *   `promotions`: A list of promotion identifiers linked to payment methods that can be applied to this order. The names of these promotions are identical to the payment method names.
        *   This list is optional. If not defined, only point-based promotions can be applied.
        *   Even if a card from the available payment methods is not defined in the `promotions` list for an order, it can still be used to pay for that order. However, its associated promotion cannot be applied in such a scenario.

2.  **`paymentmethods.json`**: List of payment methods owned by the customer:
    ```json
    [
        {"id": "PUNKTY", "discount": "15", "limit": "100.00"},
        {"id": "mZysk", "discount": "10", "limit": "180.00"},
        {"id": "BosBankrut", "discount": "5", "limit": "200.00"}
    ]
    ```
    Where:
    *   `id`: Name of the payment method / promotion. Always "PUNKTY" for loyalty points.
    *   `discount`: Integer representing the percentage discount.
    *   `limit`: Maximum amount available in this payment method (with precision to two decimal places).

## Algorithm Description

The algorithm is a variation of a greedy approach, iterating through orders and trying to select the best possible payment 
option for each to maximize the discount, considering defined heuristics, predefined constraints, and priorities.

### Phase 1: Main Payment Optimization

1.  **Sorting Orders:**
    Orders are sorted in descending order by their `value`.
2.  **Iterating Through Orders:** For each order, the following steps are taken:
    *  **Analysis of Available Promotions:** All possible payment strategies are identified (full payment with a promotional card, full payment with "PUNKTY" points, partial payment with "PUNKTY\_10" points along with its 10% discount) along with their corresponding percentage discounts.
    *  **Selection of the Best Strategy:** The strategy offering the highest percentage discount is chosen.
        *   **No Tie:** If one strategy is clearly the best, the order is paid using this method.
        *   **Tie Handling:**
            *   **Tie: Full payment with "PUNKTY" vs. Card:** Payment with points ("PUNKTY") is always chosen (prioritizing minimizing card usage if the discount is the same).
            *   **Tie: Card vs. Card (two different cards offer identical discounts):**
                1.  The card assigned as a promotion to fewer remaining, unpaid orders is chosen (to save "more common" promotions for future orders).
                2.  In case of a further tie, the card with the smaller current fund limit is chosen (to save cards with larger capacity).
            *   **Tie: "PUNKTY\_10" vs. Card (partial points strategy yields the same discount as full card payment):**
                1.  The "PUNKTY\_10" strategy is always chosen (priority for points).
                2.  **Choosing a card for the remaining payment:**
                    *   **If cards with sufficient payment limit exist:** 
                        *   A card that is *not* a promotion for any other unpaid order ("safe card") is preferred. If there are multiple such cards, the one with the smaller remaining limit is chosen.
                        *   If no "safe cards" are available, the card (from all defined cards) with the lowest discount is chosen.
                        *   In case of further ties when choosing the top-up card, criteria similar to the Card vs. Card tie are applied (rarity in future promotions, smaller limit).
                    *   **If no cards have a sufficient limit for the standard "PUNKTY\_10" top-up:** 
                        * The algorithm tries to find a card with the maximum available limit (which is less than the required top-up amount). If there are multiple such cards, it selects the best one according to the criteria defined above in the "If cards with sufficient payment limit exist" section. 
                        * The remainder (the difference between the order value after the 10% discount and the limit of the chosen card) is attempted to be covered by points.
                        * If the algorithm reaches this point and the remaining points are insufficient to cover the remaining order amount, the algorithm terminates without finding a solution, as it has reached a point where it cannot select any payment method for the currently processed order.

    c.  **State Update:** After making a choice and paying for the order, the limits of the used payment methods and an internal `ordersAmount` counter for promotional cards (indicating for how many more orders a given card can be a promotion) are updated.

### Phase 2: Spending Remaining Points (Minimizing Card Payments)

After processing all orders in Phase 1, if the customer has unused loyalty points, the algorithm attempts to spend them:

1.  **Condition:** This phase is triggered only if all orders were paid in Phase 1 and there are remaining points.
2.  **Backward Iteration:** The algorithm iterates through orders in the reverse order of their processing (from the last paid to the first).
3.  **Identifying Candidates:** It searches for orders that were paid using the "PUNKTY\_10" strategy (i.e., partially with points, partially with a card).
4.  **Replacing Card Payment with Points Payment:** For a found order:
    *   The amount paid by card under the "PUNKTY\_10" strategy is determined.
    *   The algorithm "refunds" the maximum possible amount to the used card (not more than originally paid with it and not more than the currently available points).
    *   This refunded amount is then "paid" using the remaining loyalty points.
5.  **Phase Completion:** The process continues until all loyalty points are exhausted or all orders have been reviewed.

### Algorithm Output

The `optimize()` method returns:
*   `true`: If all orders were successfully paid.
*   `false`: If, during processing, the algorithm encountered an order that could not be paid with any available combination of payment methods while respecting their limits.

If the `optimize()` method returns `true`, statistics for each payment method will be printed to standard output in the format `<payment_method_id> <amount_spent>`, e.g.:
```
PUNKTY 150.0
SuperKarta 150.0
ZwyklaKarta 0.0
MegaBank 170.0
```

## Running the Application with provided JAR file

The application is run from the command line using Java 21:

```bash
java -jar /path/to/your/application/ExpenseOptimizer.jar /path/to/orders.json /path/to/paymentmethods.json
```

Replace `/path/to/your/application/ExpenseOptimizer.jar` with the actual path to your JAR file.\
Replace `/path/to/orders.json` and `/path/to/paymentmethods.json` with the actual paths to your input JSON files.

## Building and running the Application with Maven
If you do not want to use provided JAR file, or you want to generate Javadoc documentation or Jacoco report,
you need to use Maven building tool.

### Building with Maven

1.  Clone the repository or download the source code.
2.  Navigate to the root directory of the project in your terminal.
3.  Run the following Maven command to compile the code and package it into a JAR file:
    ```bash
    mvn clean package
    ```
4.  The executable JAR file will be created in the `target/` directory (e.g., `target/ExpenseOptimizer-1.0-SNAPSHOT.jar`).
5.  You can now use this JAR file and rung application as described in `Running the Application with provided JAR file` section

### Generating Javadoc

To generate Javadoc for the project:
1.  Ensure you have built the project at least once.
2.  Run the following Maven command:
    ```bash
    mvn javadoc:javadoc
    ```
3.  The Javadoc HTML files will be generated in `target/site/apidocs/`. Open `index.html` in your browser to view the documentation.

### Generating Jacoco Code Coverage Report

To generate a code coverage report using Jacoco:
1.  Run the following Maven command to execute tests and generate the report:
    ```bash
    mvn clean test jacoco:report
    ```
2.  The Jacoco report will be generated in `target/site/jacoco/`. Open `index.html` in your browser to view the coverage details.