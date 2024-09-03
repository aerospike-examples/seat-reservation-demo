package com.aerospike.demos.seat_reservation_demo.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aerospike.demos.seat_reservation_demo.dto.ResetConcertRequest;
import com.aerospike.demos.seat_reservation_demo.model.Event;
import com.aerospike.demos.seat_reservation_demo.model.Venue;
import com.aerospike.demos.seat_reservation_demo.services.AerospikeService;
import com.aerospike.demos.seat_reservation_demo.services.EventService;
import com.aerospike.demos.seat_reservation_demo.services.VenueService;

/**
 * Implements pseudo methods not related to business functionality.
 */
@RestController()
@RequestMapping("/rpc")
public class RpcController {
    @Autowired
    private AerospikeService aerospikeService;
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private VenueService venueService;
    
    // ---------------------------------------
    // RPC calls
    // ---------------------------------------
    // Call to reset a concert to the defaul
    @PostMapping("/init") 
    public void resetAll() {
        // Reset the database to the default
        aerospikeService.resetAll();
        initializeDataModel();
    }

    // POST /rpc/resetConcert
    //-concert_id=1234
    @PostMapping("/resetConcert")
    public ResponseEntity resetConcert(@RequestBody ResetConcertRequest resetConcertRequest) {
        Optional<Event> event = eventService.loadEvent(resetConcertRequest.getConcertId());
        if (event.isPresent()) {
            eventService.clearAllSeats(event.get());
            return ResponseEntity.ok(null);
        }
        else {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private void initializeDataModel() {
        // Initialize a venue with 12 Sections
        Venue madisonSquareVenue = new Venue();
        madisonSquareVenue.setAddress("New York");
        madisonSquareVenue.setCity("New York");
        madisonSquareVenue.setCountry("USA");
        madisonSquareVenue.setDescription("Madison Square Garden, New York");
        madisonSquareVenue.setId("Madison-Square-Garden");
        madisonSquareVenue.setName("Madison Square Garden");
        madisonSquareVenue.setPostalCode("10001");
        // Define 12 equal sections with 24 rows of 24 seats each
        madisonSquareVenue.addMultipleIdenticalSections(12, 24, 24);
        venueService.saveVenue(madisonSquareVenue, null);
        
        Venue ethiadStadium = new Venue();
        ethiadStadium.setAddress("Etihad Campus");
        ethiadStadium.setCity("Manchester");
        ethiadStadium.setCountry("UK");
        ethiadStadium.setDescription("Etihad Campus, Manchester M11 3FF, United Kingdom");
        ethiadStadium.setId("Ethiad-Stadium");
        ethiadStadium.setName("Ethiad Stadium");
        ethiadStadium.setPostalCode("M11 3FF");
        // Define 12 equal sections with 24 rows of 24 seats each
        ethiadStadium.addMultipleIdenticalSections(12, 24, 24);
        venueService.saveVenue(ethiadStadium, null);
        
        Event event = Event.builder()
                .artist("Oasis")
                .category("Music")
                .date(new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli()))
                .id("Oasis-one-day")
                .subCategory("Rock")
                .title("Oasis in Concert")
                .venue(madisonSquareVenue)
                .build();
        eventService.saveEvent(event);
        
        event = Event.builder()
                .artist("Oasis")
                .category("Music")
                .date(new Date(Instant.now().plus(2, ChronoUnit.DAYS).toEpochMilli()))
                .id("Oasis-two-day")
                .subCategory("Rock")
                .title("Oasis in Concert")
                .venue(ethiadStadium)
                .build();
        eventService.saveEvent(event);
        
        event = Event.builder()
                .artist("Taylor Swift")
                .category("Music")
                .date(new Date(Instant.now().plus(3, ChronoUnit.DAYS).toEpochMilli()))
                .id("Switft-three-day")
                .subCategory("Pop")
                .title("Swifties Tour")
                .venue(madisonSquareVenue)
                .build();
        eventService.saveEvent(event);
        
        event = Event.builder()
                .artist("Taylor Swift")
                .category("Music")
                .date(new Date(Instant.now().plus(4, ChronoUnit.DAYS).toEpochMilli()))
                .id("Swift-four-day")
                .subCategory("Pop")
                .title("Swifties Tour")
                .venue(ethiadStadium)
                .build();
        eventService.saveEvent(event);
        
        event = Event.builder()
                .artist("Ed Sheeran")
                .category("Music")
                .date(new Date(Instant.now().plus(5, ChronoUnit.DAYS).toEpochMilli()))
                .id("Sheeran-five-day")
                .subCategory("Pop Folk")
                .title("Ed Sheeran Live")
                .venue(madisonSquareVenue)
                .build();
        eventService.saveEvent(event);
        
    }
}
