package dev.aerospike.ticketfaster.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Seat {
    private int sectionId;
    private int row;
    private int seatNumber;

    public String toString() {
        return String.format("%d-%d-%d", sectionId, row, seatNumber);
    }
    
    public static Seat fromString(String value) {
        String[] parts = value.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException(String.format(
                    "Expected seat in the format <sectionId>-<row>-<seatNum> but received %s", value));
        }
        return new Seat(Integer.valueOf(parts[0]),
                Integer.valueOf(parts[1]),
                Integer.valueOf(parts[2]));
    }
}
