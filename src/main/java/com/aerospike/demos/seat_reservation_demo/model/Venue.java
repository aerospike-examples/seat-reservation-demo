package com.aerospike.demos.seat_reservation_demo.model;

import lombok.Data;

@Data
public class Venue {
    private String id;
    private String name;
    private String description;
    private long numRows;
    private long seatCount;
    private String address;
    private String city;
    private String country;
    private String postalCode;
}
