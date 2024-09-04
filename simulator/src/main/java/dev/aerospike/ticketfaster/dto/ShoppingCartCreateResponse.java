package dev.aerospike.ticketfaster.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.aerospike.ticketfaster.model.Seat;
import dev.aerospike.ticketfaster.model.ShoppingCart;
import lombok.Value;

import java.util.List;

@Value
public class ShoppingCartCreateResponse {
    private final String shoppingCartId;
    private final String concertId;
    private final List<String> seats;

    @JsonCreator
    public ShoppingCartCreateResponse(
            @JsonProperty("shoppingCartId") String shoppingCartId,
            @JsonProperty("concertId") String concertId,
            @JsonProperty("seats") List<String> seats) {
        this.shoppingCartId = shoppingCartId;
        this.concertId = concertId;
        this.seats = seats;
    }

    public static ShoppingCartCreateResponse from(ShoppingCart cart) {
        if (cart == null) {
            return null;
        } else {
            ShoppingCartCreateResponse result = new ShoppingCartCreateResponse(
                    cart.getId(),
                    cart.getEventId(),
                    cart.getSeats().stream().map(Seat::toString).toList());
            return result;
        }
    }
}
