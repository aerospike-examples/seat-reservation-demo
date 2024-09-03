package dev.aerospike.ticketfaster;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.aerospike.ticketfaster.model.Event;
import dev.aerospike.ticketfaster.model.Seat;
import dev.aerospike.ticketfaster.model.Section;
import dev.aerospike.ticketfaster.model.ShoppingCart;
import dev.aerospike.ticketfaster.model.Venue;
import dev.aerospike.ticketfaster.service.EventService;
import dev.aerospike.ticketfaster.service.ShoppingCartService;
import dev.aerospike.ticketfaster.service.VenueService;

@SpringBootTest
public class EventServiceTest {
    @Autowired
    private EventService eventService;
    
    @Autowired
    private VenueService venueService;
    
    @Autowired
    private ShoppingCartService bookingService;
    
    private void printSeatMap(byte[][][] data) {
        for (int section = 0; section < data.length; section++) {
            System.out.println("**** Section " + (1+section) + " ****");
            for (int row = 0; row < data[section].length; row++) {
                System.out.printf("%04d: ", row+1);
                for (int seatNum = 0; seatNum < data[section][row].length; seatNum++) {
                    System.out.printf("%d:%d ", seatNum+1, data[section][row][seatNum]);
                }
                System.out.println();
            }
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
        venue.setPostalCode("80001");
        venue.addSection(Section.builder().name("Section").numRows(24).seatsPerRow(24).build());
        venueService.saveVenue(venue, null);
        
        Venue readVenue = venueService.loadVenue(venue.getId()).get();
        Assertions.assertTrue(readVenue.equals(venue));
        
        Event event = Event.builder()
                .date(new Date())
                .category("MUSIC")
                .id("1234567890")
                .subCategory("ROCK")
                .title("Queen Revival Concert")
                .url("http://testUrl")
                .venue(readVenue)
                .build();
        
        eventService.saveEvent(event);
        
        Optional<Event> readEvent = eventService.loadEvent(event.getId());
        Assertions.assertTrue(readEvent.isPresent());
        Assertions.assertTrue(readEvent.get().equals(event));
        
        eventService.clearAllSeats(event);
        
        System.out.println("\n*** Seat map prior to booking ***");
        printSeatMap(eventService.getAvailableSeats(event));
        
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setCreated(new Date());
        shoppingCart.setCustId(123);
        shoppingCart.setEventId(event.getId());
        shoppingCart.setId("1244");
        
        bookingService.addSeatsToCart(shoppingCart, new Seat(0, 3, 8), new Seat(0, 3, 9));
        System.out.println("\n*** Seat map after booking two seats ***");
        printSeatMap(eventService.getAvailableSeats(event));
        
        bookingService.purchaseBooking(shoppingCart, null);
        System.out.println("\n*** Seat map after purchasing two seats ***");
        printSeatMap(eventService.getAvailableSeats(event));
        
    }
}
