package com.aerospike.demos.seat_reservation_demo;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.aerospike.demos.seat_reservation_demo.model.Event;
import com.aerospike.demos.seat_reservation_demo.model.Seat;
import com.aerospike.demos.seat_reservation_demo.model.ShoppingCart;
import com.aerospike.demos.seat_reservation_demo.model.Venue;
import com.aerospike.demos.seat_reservation_demo.services.EventService;
import com.aerospike.demos.seat_reservation_demo.services.ShoppingCartService;
import com.aerospike.demos.seat_reservation_demo.services.VenueService;

@SpringBootTest
public class EventServiceTest {
    @Autowired
    private EventService eventService;
    
    @Autowired
    private VenueService venueService;
    
    @Autowired
    private ShoppingCartService bookingService;
    
    private void printSeatMap(byte[][] data) {
        for (int row = 0; row < data.length; row++) {
            System.out.printf("%04d: ", row+1);
            for (int seatNum = 0; seatNum < data[row].length; seatNum++) {
                System.out.printf("%d:%d ", seatNum+1, data[row][seatNum]);
            }
            System.out.println();
        }
    }

    @Test
    void runTest() {
        Venue venue = new Venue();
        venue.setAddress("123 Main St");
        venue.setCity("Denver");
        venue.setCountry("USA");
        venue.setId("State Center");
        venue.setName(venue.getId());
        venue.setNumRows(10);
        venue.setPostalCode("80001");
        venue.setSeatCount(15);
        venueService.saveVenue(venue, null);
        
        Venue readVenue = venueService.loadVenue(venue.getId());
        Assertions.assertTrue(readVenue.equals(venue));
        
        Event event = new Event();
        event.setDate(new Date());
        event.setCategory("MUSIC");
        event.setId("1234567890");
        event.setSubCategory("ROCK");
        event.setTitle("Queen Revival Concert");
        event.setUrl("http://testUrl");
        event.setVenue(readVenue);
        
        eventService.saveEvent(event);
        
        System.out.println("\n*** Seat map prior to booking ***");
        printSeatMap(eventService.getAvailableSeats(event));
        
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setCreated(new Date());
        shoppingCart.setCustId(123);
        shoppingCart.setEventId(event.getId());
        shoppingCart.setId("1244");
        
        bookingService.addSeatsToBooking(shoppingCart, new Seat(3, 8), new Seat(3, 9));
        System.out.println("\n*** Seat map after booking two seats ***");
        printSeatMap(eventService.getAvailableSeats(event));
        
        bookingService.purchaseBooking(shoppingCart, null);
        System.out.println("\n*** Seat map after purchasing two seats ***");
        printSeatMap(eventService.getAvailableSeats(event));
        
    }
}
