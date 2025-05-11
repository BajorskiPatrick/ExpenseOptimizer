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
 * Główna klasa aplikacji odpowiedzialna za uruchomienie procesu optymalizacji płatności.
 */
public class Main {
    /**
     * Główna metoda uruchamiająca aplikację.
     *
     * @param args Argumenty wejściowe: ścieżki do plików JSON z zamówieniami i metodami płatności
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Musisz podac dwa argumenty: sciezki do plikow JSON");
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
                pointsMethod = new PaymentMethod(-1, -1.0);
            }
        } catch (FileNotFoundException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        } catch (IOException e) {
            System.err.println("Wyjatek przy odczycie pliku: " + e.getMessage());
            return;
        }

        if (orders.isEmpty()) {
            System.out.println("W pliku json nie ma zadnych zamowien do oplacenia!");
            return;
        }
        else if (payments.isEmpty()) {
            System.out.println("W pliku json nie ma zdefiniowanych zadnych metod platnosci pomimo, ze podano zamowienia do oplacenia!");
            return;
        }

        ExpenseOptimizer optimizer = new ExpenseOptimizer(orders, payments, pointsMethod);
        boolean result;
        try {
            result = optimizer.optimize();
        }
        catch (IllegalArgumentException e) {
            System.out.println("W trakcie wykonywania algorytmu nastapil wyjatek i algorytm zakonczyl działanie:");
            System.err.println(e.getMessage());
            System.out.println();
            result = false;
        }

        if (result) {
            System.out.println("Optymalizacja ukonczona pomyslnie\n");
            System.out.println("-------Wyniki algorytmu-------");
            if (pointsMethod.getId().equals("PUNKTY")) {
                System.out.println(pointsMethod.printSpending());
            }
            for (PaymentMethod paymentMethod : payments.values()) {
                System.out.println(paymentMethod.printSpending());
            }
            System.out.println();
        }
        else {
            System.out.println("Niestety, dla dostarczonych danych, algorytmowi nie udalo sie dojsc do rozwiazania :(");
        }
    }
}
