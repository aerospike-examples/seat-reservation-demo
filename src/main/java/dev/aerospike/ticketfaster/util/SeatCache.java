package dev.aerospike.ticketfaster.util;

import java.util.ArrayList;
import java.util.List;

import dev.aerospike.ticketfaster.model.Event;
import dev.aerospike.ticketfaster.model.Seat;
import dev.aerospike.ticketfaster.model.SeatStatus;
import dev.aerospike.ticketfaster.model.Section;

/**
 * Cache the state of seats at an event. This cache is used ONLY to simulate picking random seats,
 * which will not be needed in production.
 */
public class SeatCache {
    public static class NotEnoughSeatsException extends RuntimeException {
        public NotEnoughSeatsException(int requested, int available) {
            super(String.format("Requested %d seats, but only %d available", requested, available));
        }
    }
    
    private byte[][][] cache;
    private final int totalSeats;
    private int availableSeats;
    private final List<Section> seatMap;
    
    public SeatCache(Event event, byte[][][] map) {
        this.cache = map;
        this.seatMap = event.getVenue().getSeatMap();
        this.totalSeats = countSeats();
    }
    
    private int countSeats() {
        int totalSeats = 0;
        this.availableSeats = 0;
        
        for (int sectionId = 0; sectionId < seatMap.size(); sectionId++) {
            Section section = seatMap.get(sectionId);
            totalSeats += section.getNumRows() * section.getSeatsPerRow();
            for (int row = 0; row < section.getNumRows(); row++) {
                for (int seatNum = 0; seatNum < section.getSeatsPerRow(); seatNum++) {
                    byte status = cache[sectionId][row][seatNum];
                    if (status == SeatStatus.AVAILABLE.getValue()) {
                        availableSeats++;
                    }
                }
            }
        }
        return totalSeats;
    }
    
    public synchronized void refresh(byte[][][] map) {
        this.cache = map;
        countSeats();
    }
    
    public void updateSeat(int sectionId, int row, int seatNum, SeatStatus newStatus) {
        cache[sectionId][row][seatNum] = (byte)newStatus.getValue();
        if (newStatus == SeatStatus.AVAILABLE) {
            availableSeats++;
        }
        else if (newStatus == SeatStatus.RESERVED) {
            availableSeats--;
        }
    }
    
    public boolean isAvailable(int sectionId, int row, int seatNum) {
        return cache[sectionId][row][seatNum] == SeatStatus.AVAILABLE.getValue();
    }
    
    public synchronized List<Seat> getRandomAvailableSeats(int count) {
        if (count > availableSeats) {
            throw new NotEnoughSeatsException(count, availableSeats); 
        }
        List<Seat> retrievedSeats = new ArrayList<>();
        RandomIterator sectionIterator = new RandomIterator(seatMap.size());
        RandomIterator rowIterator = new RandomIterator(0);
        RandomIterator seatIterator = new RandomIterator(0);
        
        while (sectionIterator.hasNext()) {
            int thisSection = sectionIterator.next();
            Section section = seatMap.get(thisSection);
            rowIterator.reset(section.getNumRows());
            while (rowIterator.hasNext()) {
                int thisRow = rowIterator.next();
                seatIterator.reset(section.getSeatsPerRow());
                while (seatIterator.hasNext()) {
                    int thisSeat = seatIterator.next();
                    if (isAvailable(thisSection, thisRow, thisSeat)) {
                        retrievedSeats.add(new Seat(thisSection, thisRow, thisSeat));
                        if (retrievedSeats.size() == count) {
                            return retrievedSeats;
                        }
                    }
                }
                
            }
        }
        // Shouldn't happen
        throw new NotEnoughSeatsException(count, availableSeats);
    }
}
