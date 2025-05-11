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

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("You need to provide two arguments: json file paths");
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
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            return;
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        ExpenseOptimizer optimizer = new ExpenseOptimizer(orders, payments, pointsMethod);
        boolean result = optimizer.optimize();
        if (result) {
            System.out.println("Optimization completed successfully!\n");
            System.out.println("-------Optimization result-------");
            System.out.println(pointsMethod.printSpending());
            for (PaymentMethod paymentMethod : payments.values()) {
                System.out.println(paymentMethod.printSpending());
            }
            System.out.println();
        }
        else {
            System.out.println("Regrettably, the algorithm failed to optimize payments method for this example :(");
        }
    }
}
