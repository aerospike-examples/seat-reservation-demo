package dev.aerospike.ticketfaster.model;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Event {
    private String id;
    private String title;
    private String url;
    private String category;
    private String subCategory;
    private String artist;
    private Venue venue;
    private Date date;
}
