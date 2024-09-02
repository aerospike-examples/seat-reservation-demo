package com.aerospike.demos.seat_reservation_demo.dto;

import java.util.List;

import com.aerospike.demos.seat_reservation_demo.model.Seat;
import com.aerospike.demos.seat_reservation_demo.model.ShoppingCart;

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
