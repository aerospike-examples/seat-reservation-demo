package com.aerospike.demos.seat_reservation_demo.model;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class Booking {
    
    public enum Status {
        PENDING,
        PURCHASED
    }
    
    private String eventId;
    private long custId;
    private Status status = Status.PENDING;
    private List<Seat> seats;
    private Date created;
}
