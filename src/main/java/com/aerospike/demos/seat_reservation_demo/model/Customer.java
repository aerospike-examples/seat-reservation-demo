package com.aerospike.demos.seat_reservation_demo.model;

import java.util.Date;

import lombok.Data;

@Data
public class Customer {
    private String firstName;
    private String lastName;
    private Date dateOfBirth;
    private long id;
}
