package com.aerospike.demos.seat_reservation_demo.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class Booking {
    
    public enum Status {
        PENDING,
        PURCHASED
    }
    private String id;
    private String eventId;
    private long custId;
    private Status status = Status.PENDING;
    private Set<Seat> seats = new HashSet<>();
    private Date created;
}
