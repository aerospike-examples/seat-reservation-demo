package dev.aerospike.ticketfaster.controller;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;

import dev.aerospike.ticketfaster.dto.ShoppingCartCreateRequest;
import dev.aerospike.ticketfaster.dto.ShoppingCartCreateResponse;
import dev.aerospike.ticketfaster.model.Event;
import dev.aerospike.ticketfaster.model.Seat;
import dev.aerospike.ticketfaster.model.ShoppingCart;
import dev.aerospike.ticketfaster.service.EventService;
import dev.aerospike.ticketfaster.service.NotifierService;
import dev.aerospike.ticketfaster.service.ShoppingCartService;
import dev.aerospike.ticketfaster.util.SeatCache.NotEnoughSeatsException;

@RestController
public class ConcertController {
    @Autowired
    private EventService eventService;
    
    @Autowired
    private ShoppingCartService cartService;
    
    @Autowired
    private NotifierService notifierService;
    
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
    
    @GetMapping("/concerts/getAll/")
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
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
        
        try {
            int seatCount = createRequest.getRandomSeatQuantity();
            if (seatCount > 0) {
                cartService.createShoppingCart(shoppingCart);
                List<Seat> seats = cartService.findAndReserveRandomSeats(shoppingCart, concertId, seatCount);
                shoppingCart.getSeats().addAll(seats);
            }
            else {
                Set<Seat> seats = new HashSet<>();
                for (String seatStr : createRequest.getSeats()) {
                    seats.add(Seat.fromString(seatStr));
                }
                shoppingCart.setSeats(seats);
                cartService.createShoppingCart(shoppingCart);
            }
            return ResponseEntity.created(null).body(ShoppingCartCreateResponse.from(shoppingCart));
        }
        catch (NotEnoughSeatsException nese) {
            return ResponseEntity.notFound().build();
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
    
    @GetMapping("testBytes")
    public byte[][] bytes() {
        return new byte[][] {new byte[] {1,2,3,4,5,6}, new byte[] { 5,4,3}};
    }
    
    @GetMapping("/concerts/{concertId}/seats")
    public List<List<List<Integer>>> getSeatStauses(@PathVariable String concertId) {
        Optional<Event> event = eventService.loadEvent(concertId);
        if (event.isPresent()) {
            byte[][][] map = eventService.getAvailableSeats(event.get());
            List<List<List<Integer>>> venueMap = new ArrayList<>();
            for (int sectionId = 0; sectionId < map.length; sectionId++) {
                byte[][] sectionSeats = map[sectionId];
                List<List<Integer>> sectionMap = new ArrayList<>();
                venueMap.add(sectionMap);
                for (int row = 0; row < sectionSeats.length; row++) {
                    byte[] rowSeats = map[sectionId][row];
                    List<Integer> rowMap = new ArrayList<>();
                    sectionMap.add(rowMap);
                    for (int seatNum = 0; seatNum < rowSeats.length; seatNum++ ) {
                        rowMap.add((int)map[sectionId][row][seatNum]);
                    }
                }
            }
            return venueMap;
        }
        else {
            return null;
        }
    }
        
    @GetMapping("/register")
    public @ResponseBody SseEmitter register() {
        return notifierService.register();
    }
}
