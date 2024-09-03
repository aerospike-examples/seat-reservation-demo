package com.aerospike.demos.seat_reservation_demo.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ShoppingCartCreateRequest {
    private String id;
    private long userId;
    private List<String> seats = new ArrayList<>();
}
