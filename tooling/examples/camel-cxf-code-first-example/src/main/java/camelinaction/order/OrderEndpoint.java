package camelinaction.order;

import javax.jws.WebService;

@WebService
public interface OrderEndpoint {
    String order(String partName, int amount, String customerName);
}
