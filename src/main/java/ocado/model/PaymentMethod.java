package ocado.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentMethod {
    private String id;
    private int discount;
    private double limit;
    private int ordersAmount;
    private double spending;


    public void incrementOrdersAmount() {
        ordersAmount++;
    }

    public void spend(double amount) {
        spending += amount;
        limit -= amount;
    }

    public void getMoneyBack(double amount) {
        spending -= amount;
        limit += amount;
    }

    public String printSpending() {
        return id + " " + spending;
    }

    @Override
    public String toString() {
        return "PaymentMethod{" +
                "id='" + id + '\'' +
                ", discount=" + discount +
                ", limit=" + limit +
                ", ordersAmount=" + ordersAmount +
                ", spending=" + spending +
                '}';
    }
}
