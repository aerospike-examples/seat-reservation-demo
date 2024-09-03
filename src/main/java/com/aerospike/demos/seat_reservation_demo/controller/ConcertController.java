package com.aerospike.demos.seat_reservation_demo.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import com.aerospike.demos.seat_reservation_demo.dto.ShoppingCartCreateRequest;
import com.aerospike.demos.seat_reservation_demo.dto.ShoppingCartCreateResponse;
import com.aerospike.demos.seat_reservation_demo.model.Event;
import com.aerospike.demos.seat_reservation_demo.model.Seat;
import com.aerospike.demos.seat_reservation_demo.model.ShoppingCart;
import com.aerospike.demos.seat_reservation_demo.services.EventService;
import com.aerospike.demos.seat_reservation_demo.services.ShoppingCartService;

@RestController
public class ConcertController {
    @Autowired
    private EventService eventService;
    
    @Autowired
    private ShoppingCartService cartService;
    
    /**
     * Load a set of events and the associated venues from the /data/ticketmaster_events_2023-08-02.json file.
     * <p>
     * While this contains historical events and real venues, it is not super useful as the venues do not contain
     * descriptions of rows of seats so cannot effectively be reused here.
     * @return
     * @throws IOException
     */
    @GetMapping("/demo/loadEvents")
    public String loadEvents() throws IOException {
        InputStream is = getClass().getResourceAsStream("/data/ticketmaster_events_2023-08-02.json");
        try {
            eventService.loadEventAndVenueData(is);
            return "Success";
        }
        catch (IOException | InterruptedException ioe) {
            System.err.printf("Error loading data from file %s: %s (%s)", "data/ticketmaster_events_2023.json", ioe.getMessage(), ioe.getClass());
            ioe.printStackTrace();
            return "Failed";
        }
    }
    
    @PutMapping(path = "/demo/event/save", 
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> saveEvent(@RequestBody Event event) {
        try {
            eventService.saveEvent(event);
            return ResponseEntity.ok(true);
        }
        catch (Exception e) {
            return ResponseEntity.internalServerError().body(false);
        }
    }
    
    @GetMapping("/demo/eventsInDateRange/{startDate}/{endDate}")
    public List<Event> getEventsInDateRange(
            @PathVariable(name = "startDate") long startDate, 
            @PathVariable(name = "endDate") long endDate) {
        
        return eventService.getEventsInDateRange(
                startDate == 0 ? null : new Date(startDate), 
                endDate == 0 ? null : new Date(endDate));
    }
    
    @PostMapping("/concerts/{concertId}/shopping-carts")
    public ResponseEntity<ShoppingCartCreateResponse> createCart(@PathVariable String concertId, @RequestBody ShoppingCartCreateRequest createRequest) {
        // TODO: Validate the seat selection against the event / venue
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setEventId(concertId);
        shoppingCart.setId(createRequest.getId());
        shoppingCart.setCustId(createRequest.getUserId());
        Set<Seat> seats = new HashSet<>();
        for (String seatStr : createRequest.getSeats()) {
            seats.add(Seat.fromString(seatStr));
        }
        shoppingCart.setSeats(seats);
        try {
            cartService.createShoppingCart(shoppingCart);
            return ResponseEntity.created(null).body(ShoppingCartCreateResponse.from(shoppingCart));
        }
        catch (AerospikeException ae) {
            if (ae.getResultCode() == ResultCode.OP_NOT_APPLICABLE) {
                return ResponseEntity.badRequest().build();
            }
            else {
                throw ae;
            }
        }
    }
    
    @GetMapping("/concerts/{concertId}/shopping-carts/{cartId}")
    public ShoppingCartCreateResponse getCart(
                @PathVariable String concertId,
                @PathVariable String cartId
            ) {
        return ShoppingCartCreateResponse.from(cartService.loadCart(cartId).get());
    }
    
    // ## Add items to my Shopping Cart
    // POST /concerts/1234/shopping-carts/f7e1f2d4-8c3d-45e6-8d77-2d3e1c2b4f78/seats/a32
    @PostMapping("/concerts/{concertId}/shopping-carts/{cartId}/seats")
    public ResponseEntity addSeatsToCart(
                @PathVariable String concertId, 
                @PathVariable String cartId, 
                @RequestBody ShoppingCartCreateRequest createRequest) {
        
        Optional<ShoppingCart> shoppingCart = cartService.loadCart(cartId);
        if (shoppingCart.isPresent()) {
            List<Seat> seats = new ArrayList<>();
            for (String seatStr : createRequest.getSeats()) {
                seats.add(Seat.fromString(seatStr));
            }
            
            try {
                cartService.addSeatsToCart(shoppingCart.get(), seats.toArray(new Seat[0]));
                return ResponseEntity.noContent().build();
            }
            catch (AerospikeException ae) {
                if (ae.getResultCode() == ResultCode.OP_NOT_APPLICABLE) {
                    return ResponseEntity.badRequest().build();
                }
                else {
                    throw ae;
                }
            }
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    // ## Remove items from my Shopping Cart
    // DELETE /concerts/1234/shopping-carts/f7e1f2d4-8c3d-45e6-8d77-2d3e1c2b4f78/seats/a32
    @DeleteMapping("/concerts/{concertId}/shopping-carts/{cartId}/seats")
    public ResponseEntity removeSeatsFromCart(
                @PathVariable String concertId, 
                @PathVariable String cartId, 
                @RequestBody ShoppingCartCreateRequest createRequest) {
        
        Optional<ShoppingCart> shoppingCart = cartService.loadCart(cartId);
        if (shoppingCart.isPresent()) {
            List<Seat> seats = new ArrayList<>();
            for (String seatStr : createRequest.getSeats()) {
                seats.add(Seat.fromString(seatStr));
            }
            
            try {
                cartService.removeSeatsFromCart(shoppingCart.get(), seats.toArray(new Seat[0]));
                return ResponseEntity.noContent().build();
            }
            catch (AerospikeException ae) {
                if (ae.getResultCode() == ResultCode.OP_NOT_APPLICABLE) {
                    return ResponseEntity.badRequest().build();
                }
                else {
                    throw ae;
                }
            }
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    // ## Abandon / clear my Shopping Cart
    // DELETE /concerts/1234/shopping-carts/f7e1f2d4-8c3d-45e6-8d77-2d3e1c2b4f78
    @DeleteMapping("/concerts/{concertId}/shopping-carts/{cartId}")
    public ResponseEntity clearCart(
                @PathVariable String concertId, 
                @PathVariable String cartId) {
        
        Optional<ShoppingCart> shoppingCart = cartService.loadCart(cartId);
        if (shoppingCart.isPresent()) {
            try {
                cartService.clearShoppingCart(shoppingCart.get());
                return ResponseEntity.noContent().build();
            }
            catch (AerospikeException ae) {
                if (ae.getResultCode() == ResultCode.OP_NOT_APPLICABLE) {
                    return ResponseEntity.badRequest().build();
                }
                else {
                    throw ae;
                }
            }
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // ## Finalize a purchase
    // POST /concerts/1234/purchases
    // - shoppping_cart=f7e1f2d4-8c3d-45e6-8d77-2d3e1c2b4f78
    @PostMapping("/concerts/{concertId}/purchases")
    public ResponseEntity addSeatsToCart(
                @PathVariable String concertId, 
                @RequestBody ShoppingCartCreateRequest createRequest) {
        
        Optional<ShoppingCart> shoppingCart = cartService.loadCart(createRequest.getId());
        if (shoppingCart.isPresent()) {
            try {
                cartService.purchaseBooking(shoppingCart.get(), null);
                return ResponseEntity.noContent().build();
            }
            catch (AerospikeException ae) {
                if (ae.getResultCode() == ResultCode.OP_NOT_APPLICABLE) {
                    return ResponseEntity.badRequest().build();
                }
                else {
                    throw ae;
                }
            }
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/concerts/{concertId}/seats")
    public byte[][][] getSeatStauses(@PathVariable String concertId) {
        Optional<Event> event = eventService.loadEvent(concertId);
        if (event.isPresent()) {
            return eventService.getAvailableSeats(event.get());
        }
        else {
            return null;
        }
    }
}
