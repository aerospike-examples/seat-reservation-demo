package com.aerospike.demos.seat_reservation_demo.model;

import lombok.Data;

@Data
public class Venue {
    private String id;
    private String name;
    private String description;
    /** The number of rows of seats in this venue */
    private int numRows;
    /** The number of seats per row */
    private int seatCount;
    private String address;
    private String city;
    private String country;
    private String postalCode;
}
