package dev.aerospike.ticketfaster.model;

public class SeatLocation {
    private Long showId;
    private int sectionNumber;
    private int seatNumber;

    public SeatLocation(Long showId, int sectionNumber, int seatNumber) {
        this.showId = showId;
        this.sectionNumber = sectionNumber;
        this.seatNumber = seatNumber;
    }

    public Long getShowId() {
        return showId;
    }

    public int getSectionNumber() {
        return sectionNumber;
    }

    public int getSeatNumber() {
        return seatNumber;
    }
}
