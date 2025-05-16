package ocado;

import ocado.model.Order;
import ocado.model.PaymentMethod;
import ocado.optimization.ExpenseOptimizer;
import ocado.utils.JsonProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Main application class responsible for launching the payment optimization process.
 */
public class Main {
    /**
     * Main method launching the application.
     *
     * @param args Input arguments: paths to JSON files containing orders and payment methods
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("You need to provide two arguments: paths to JSON files");
            return;
        }
        File ordersFile = new File(args[0]);
        File paymentsFile = new File(args[1]);

        List<Order> orders;
        Map<String, PaymentMethod> payments;
        PaymentMethod pointsMethod;
        try {
            orders = JsonProcessor.getOrdersAsList(ordersFile);
            payments = JsonProcessor.getPaymentsAsMap(paymentsFile, orders);
            pointsMethod = payments.remove("PUNKTY");
            if (pointsMethod == null) {
                pointsMethod = new PaymentMethod("-1.00", "-1.00");
            }
        } catch (FileNotFoundException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        } catch (IOException e) {
            System.err.println("Exception occurred while reading files " + e.getMessage());
            return;
        }

        if (orders.isEmpty()) {
            System.out.println("There is no order to pay for in JSON file");
            return;
        }
        else if (payments.isEmpty()) {
            System.out.println("There are no payment methods defined in the json file even though there are orders to pay for!");
            return;
        }

        ExpenseOptimizer optimizer = new ExpenseOptimizer(orders, payments, pointsMethod);
        boolean result;
        try {
            result = optimizer.optimize();
        }
        catch (IllegalArgumentException e) {
            System.out.println("While executing the algorithm, an exception occurred and the algorithm terminated:");
            System.err.println(e.getMessage());
            System.out.println();
            result = false;
        }

        if (result) {
            System.out.println("Optimization finished successfully\n");
            System.out.println("-------Algorithm result-------");
            if (pointsMethod.getId().equals("PUNKTY")) {
                System.out.println(pointsMethod.printSpending());
            }
            for (PaymentMethod paymentMethod : payments.values()) {
                System.out.println(paymentMethod.printSpending());
            }
            System.out.println();
        }
        else {
            System.out.println("Unfortunately, for the entered data, the algorithm was unable to reach a solution :(");
        }
    }
}
