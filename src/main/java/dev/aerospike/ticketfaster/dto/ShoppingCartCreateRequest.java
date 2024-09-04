package dev.aerospike.ticketfaster.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ShoppingCartCreateRequest {
    private String id;
    private long userId;
    private int randomSeatQuantity;
    private List<String> seats = new ArrayList<>();
}
