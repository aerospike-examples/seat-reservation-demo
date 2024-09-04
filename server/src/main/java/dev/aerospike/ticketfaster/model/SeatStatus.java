package dev.aerospike.ticketfaster.model;

public enum SeatStatus {
    AVAILABLE(0),
    RESERVED(1),
    PURCHASED(2),
    NOT_AVAILABLE(3);
    
    private int value;
    private SeatStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
