package com.aerospike.demos.seat_reservation_demo.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Section {
    private int sectionId;
    private int numRows;
    private int seatsPerRow;
    private String name;
}
