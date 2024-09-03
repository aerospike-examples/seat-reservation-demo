package dev.aerospike.ticketfaster.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Section {
    private int sectionNumber;
    private int numRows;
    private int seatsPerRow;
    private String name;
}
