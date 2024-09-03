package dev.aerospike.ticketfaster.dto;

import java.util.List;

import dev.aerospike.ticketfaster.model.Seat;
import dev.aerospike.ticketfaster.model.ShoppingCart;
import lombok.Value;

@Value
public class ShoppingCartCreateResponse {
    private final String shoppingCartId;
    private final String concertId;
    private final List<String> seats;
    
    public static ShoppingCartCreateResponse from(ShoppingCart cart) {
        if (cart == null) {
            return null;
        }
        else {
            ShoppingCartCreateResponse result = new ShoppingCartCreateResponse(
                    cart.getId(), 
                    cart.getEventId(), 
                    cart.getSeats().stream().map(Seat::toString).toList());
            return result;
        }
    }
}
