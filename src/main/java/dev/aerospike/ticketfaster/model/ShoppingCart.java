package dev.aerospike.ticketfaster.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class ShoppingCart {
    
    public enum Status {
        PENDING,
        PURCHASED,
        ABANDONED
    }
    private String id;
    private String eventId;
    private long custId;
    private Status status = Status.PENDING;
    private Set<Seat> seats = new HashSet<>();
    private Date created;
    
    // TODO: Consider expiry of the shopping cart.
}
