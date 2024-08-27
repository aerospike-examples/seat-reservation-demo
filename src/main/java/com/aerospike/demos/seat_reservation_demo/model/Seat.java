package com.aerospike.demos.seat_reservation_demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Seat {
    private int row;
    private int seatNumber;

}
