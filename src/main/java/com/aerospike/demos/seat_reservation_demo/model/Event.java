package com.aerospike.demos.seat_reservation_demo.model;

import java.util.Date;

import lombok.Data;

@Data
public class Event {
    private String id;
    private String title;
    private String url;
    private String category;
    private String subCategory;
    private Venue venue;
    private Date date;
}
