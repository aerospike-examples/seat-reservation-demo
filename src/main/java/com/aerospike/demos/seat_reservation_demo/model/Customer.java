package com.aerospike.demos.seat_reservation_demo.model;

import java.util.Date;
import java.util.Set;

import lombok.Data;

@Data
public class Customer {
    private String firstName;
    private String lastName;
    private Date dateOfBirth;
    private long id;
    
    // TODO: Add in the bookings. This is not wired up to keep the bookings at the moment.
    private Set<ShoppingCart> bookings;
}
