package com.aerospike.demos.seat_reservation_demo.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Txn;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.CTX;
import com.aerospike.client.cdt.ListOrder;
import com.aerospike.client.cdt.ListPolicy;
import com.aerospike.client.cdt.ListReturnType;
import com.aerospike.client.cdt.ListWriteFlags;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.cdt.MapWriteFlags;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.Exp.Type;
import com.aerospike.client.exp.ExpOperation;
import com.aerospike.client.exp.ExpWriteFlags;
import com.aerospike.client.exp.ListExp;
import com.aerospike.client.exp.MapExp;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.demos.seat_reservation_demo.model.Customer;
import com.aerospike.demos.seat_reservation_demo.model.Event;
import com.aerospike.demos.seat_reservation_demo.model.Seat;
import com.aerospike.demos.seat_reservation_demo.model.SeatStatus;
import com.aerospike.demos.seat_reservation_demo.model.ShoppingCart;
import com.aerospike.demos.seat_reservation_demo.model.ShoppingCart.Status;
import com.aerospike.demos.seat_reservation_demo.model.Venue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class AerospikeService {
    private static final String HOSTNAME = "localhost";
    public static final String NAMESPACE = "test";
    
    public static final String CUSTOMER_SET = "customer";
    public static final String EVENT_SET = "event";
    public static final String VENUE_SET = "venue";
    public static final String EVENT_SEAT_SET = "eventSeats";
    public static final String SHOPPING_CART_SET = "cart";
    
    private IAerospikeClient client;
    
    @PostConstruct
    private void connectToDatabase() {
        this.client = new AerospikeClient(HOSTNAME, 3000);
    }
    
    @PreDestroy
    private void cleanup() {
        if (this.client != null) {
            this.client.close();
        }
    }
    
    // Convenience methods for mapping to/from database. Might replace with java object mapper
    // once MRT is supported
    public static long toAerospike(Date date) {
        return date == null ? 0L : date.getTime();
    }
    
    public static Date fromAerospike(long date) {
        return date == 0 ? null : new Date(date);
    }
    
    public void save(WritePolicy wp, Event event, Txn txn) {
        if (wp == null) {
            wp = client.copyWritePolicyDefault();
            wp.txn = txn;
        }
        wp.sendKey = true;
        Key key = new Key(NAMESPACE, EVENT_SET, event.getId());
        client.put(wp, key, 
                new Bin("date", toAerospike(event.getDate())),
                new Bin("category", event.getCategory()),
                new Bin("subCat", event.getSubCategory()),
                new Bin("venue", event.getVenue() == null ? "" : event.getVenue().getId()),
                new Bin("title", event.getTitle()),
                new Bin("url", event.getUrl())
            );
    }
    
    public void save(WritePolicy wp, Customer customer, Txn txn) {
        if (wp == null && txn != null) {
            wp = client.copyWritePolicyDefault();
            wp.txn = txn;
        }
        wp.sendKey = true;
        Key key = new Key(NAMESPACE, CUSTOMER_SET, customer.getId());
        client.put(wp, key, 
                new Bin("dob", toAerospike(customer.getDateOfBirth())),
                new Bin("firstName", customer.getFirstName()),
                new Bin("lastName", customer.getLastName())
            );
    }
    
    private Event eventFromRecord(Record thisRecord, boolean loadVenue) {
        if (thisRecord == null) {
            return null;
        }
        else {
            Event result = new Event();
            result.setCategory(thisRecord.getString("category"));
            result.setDate(fromAerospike(thisRecord.getLong("date")));
            result.setSubCategory(thisRecord.getString("subCat"));
            result.setTitle(thisRecord.getString("title"));
            result.setUrl(thisRecord.getString("url"));
            if (loadVenue) {
                String venueId = thisRecord.getString("venue");
                if (venueId != null) {
                    result.setVenue(this.readVenue(venueId));
                }
            }
            return result;
        }
    }
    public Event readEvent(String id) {
        Record thisRecord = client.get(null, new Key(NAMESPACE, EVENT_SET, id));
        return eventFromRecord(thisRecord, true);
    }

    public List<Event> getEventsInDateRange(Date startDate, Date endDate) {
        Statement stmt = new Statement();
        stmt.setNamespace(NAMESPACE);
        stmt.setSetName(EVENT_SET);
        stmt.setFilter(Filter.range("date", toAerospike(startDate), toAerospike(endDate)));
        QueryPolicy qp = new QueryPolicy();
        RecordSet recordSet = client.query(qp, stmt);
        List<Event> results = new ArrayList<>();
        while (recordSet.next()) {
            Record thisRecord = recordSet.getRecord();
            results.add(eventFromRecord(thisRecord, false));
        }
        results.sort((a,b) -> (int)(a.getDate().getTime() - b.getDate().getTime()));
        return results;
    }
    
    private Key getRowOfSeatsKey(String eventId, int row) {
        return new Key(NAMESPACE, EVENT_SEAT_SET, eventId + "|" + row);
    }
    
    /**
     * Get the status of the seats for an event. The result will be an array with the number of rows in the venue,
     * and each item in the array will be an array of the seat statuses in that row. The seat statuses will be downcast
     * to a byte for more efficient storage
     * <p>
     * For for a small theater with 4 rows and 5 seats per row, the result must be something like:
     * <pre>
     * 0: 0 0 1 2 0
     * 1: 2 2 2 2 0
     * 2: 1 1 1 2 2
     * 3: 0 0 0 0 0
     * </pre>
     * Here, there are 3 vacant seats in the first one, one reserved and not purchased and one purchased.
     * See {@code SeatStatus} for a list of possible statuses. 
     * @param event
     * @return
     */
    public byte[][] getEventSeatStatus(Event event) {
        if (event.getVenue() == null) {
            throw new IllegalArgumentException("Event must be associated with a venue");
        }
        int rows = event.getVenue().getNumRows();
        int seatsPerRow = event.getVenue().getSeatCount();
        
        Key[] keys = new Key[rows];
        for (int i = 0; i < rows; i++) {
            keys[i] = getRowOfSeatsKey(event.getId(), i);
        }
        Record[] records = client.get(null, keys);
        byte[][] seatStatuses = new byte[(int)rows][];
        for (int row = 0; row < rows; row++) {
            seatStatuses[row] = new byte[seatsPerRow];
            Record thisRecord = records[row];
            if (thisRecord != null) {
                Map<Long, Object> seatMap = (Map<Long, Object>) thisRecord.getMap("seats");
                if (seatMap != null) {
                    for (long thisSeat : seatMap.keySet()) {
                        List<Object> seatData = (List<Object>) seatMap.get(thisSeat);
                        int seatStatus = ((Long)seatData.get(0)).intValue();
                        seatStatuses[row][(int)thisSeat] = (byte)seatStatus;
                    }
                }
            }
        }
        return seatStatuses;
    }
    
    /**
     * Update the status of a seat for a customer. Seats must follow the lifecycle of 
     * AVAILABLE -> RESERVED -> PURCHASED [-> AVAILABLE] or AVAILABLE -> RESERVED -> AVAILABLE
     * <p>
     * Transitions out of RESERVED and PURCHASED statuses require the passed user to be the owner of that seat. 
     * @param event
     * @param custId
     * @param row
     * @param seatNumber
     * @param newStatus
     * @param txn
     */
    public void setSeatStatus(String eventId, long custId, int row, int seatNumber, SeatStatus newStatus, Txn txn) {
        final String SEAT_BIN = "seats";
        
        Map<Integer, Object> emptyMap = new HashMap<>();
        MapPolicy mp = new MapPolicy(MapOrder.KEY_ORDERED, MapWriteFlags.DEFAULT);
        ListPolicy lp = new ListPolicy(ListOrder.UNORDERED, ListWriteFlags.DEFAULT);
        
        List<Object> seatData = new ArrayList<>();
        seatData.add(newStatus.getValue());
        seatData.add(custId);
        
        // Seats are stored per row in a sub-ordinate object
        Key key = getRowOfSeatsKey(eventId, row);
        Exp mapExists = Exp.binExists(SEAT_BIN);
        Exp seatExists = MapExp.getByKey(MapReturnType.EXISTS, Type.BOOL, Exp.val(seatNumber), Exp.mapBin(SEAT_BIN));
        
        // This logic assumes the seat exists
        Exp seatReplacementLogic = Exp.let(
                Exp.def("seat", MapExp.getByKey(MapReturnType.VALUE, Type.LIST, Exp.val(seatNumber), Exp.mapBin(SEAT_BIN))),
                Exp.def("status", ListExp.getByIndex(ListReturnType.VALUE, Type.INT, Exp.val(0), Exp.var("seat"))),
                Exp.def("customer", ListExp.getByIndex(ListReturnType.VALUE, Type.INT, Exp.val(1), Exp.var("seat"))),
                Exp.cond(
                        // Resetting the seat back to AVAILABLE from the purchaser in state of RESERVED or PURCHASED
                        Exp.and(
                                // New status is to be set to available
                                Exp.eq(Exp.val(newStatus.getValue()), Exp.val(SeatStatus.AVAILABLE.getValue())),
                                // AND the current status is RESERVED or PURCHASED
                                Exp.or(
                                        Exp.eq(Exp.var("status"), Exp.val(SeatStatus.RESERVED.getValue())),
                                        Exp.eq(Exp.var("status"), Exp.val(SeatStatus.PURCHASED.getValue()))
                                ),
                                // AND the current owner of the seat is the same as the customer
                                Exp.eq(Exp.var("customer"), Exp.val(custId))
                        ),
                        MapExp.removeByKey(Exp.val(seatNumber), Exp.mapBin(SEAT_BIN)),
                        
                        // Setting status to RESERVED from AVAILBLE (not existing)
                        Exp.and(
                                // New status is to be set to reserved
                                Exp.eq(Exp.val(newStatus.getValue()), Exp.val(SeatStatus.RESERVED.getValue())),
                                Exp.not(seatExists)
                        ),
                        MapExp.put(mp, Exp.val(seatNumber), Exp.val(seatData), Exp.mapBin(SEAT_BIN)),
                        
                        // Setting the status to PURCHASED from RESERVED with the same ID
                        Exp.and(
                                // New status is to be set to PURCHASED
                                Exp.eq(Exp.val(newStatus.getValue()), Exp.val(SeatStatus.PURCHASED.getValue())),
                                // AND the existing status is RESERVED
                                Exp.eq(Exp.var("status"), Exp.val(SeatStatus.RESERVED.getValue())),
                                // AND the customer is the same customer id
                                Exp.eq(Exp.var("customer"), Exp.val(custId))
                        ),
//                        MapExp.put(mp, Exp.val(seatNumber), Exp.val(seatData), Exp.mapBin(SEAT_BIN)),
                        ListExp.set(lp, Exp.val(0), Exp.val(SeatStatus.PURCHASED.getValue()), Exp.mapBin(SEAT_BIN), CTX.mapKey(Value.get(seatNumber))),
                        
                        // Else it's an error
                        Exp.unknown()
                )
            );
//        Exp seatReplacementLogic = Exp.cond(Exp.eq(Exp.val(newStatus.getValue()), Exp.val(SeatStatus.AVAILABLE.getValue())), 
//                MapExp.removeByKey(Exp.val(seatNumber), Exp.mapBin(SEAT_BIN)),
//                MapExp.put(mp, Exp.val(seatNumber), Exp.val(seatData), Exp.mapBin(SEAT_BIN))
//        );
        Exp createNewSeatInNewMap = MapExp.put(mp, Exp.val(seatNumber), Exp.val(seatData), Exp.val(emptyMap));
        Exp createNewSeatInExistingMap = MapExp.put(mp, Exp.val(seatNumber), Exp.val(seatData), Exp.mapBin(SEAT_BIN));
        
        // If the seat exists, write seat data, otherwise create a new seat. 
        Exp seatLogic = Exp.cond(seatExists, seatReplacementLogic, createNewSeatInExistingMap);
        
        // If the map exists, check the seat logic, otherwise, create a new map with the seat data.
        Exp checkSeatExists = Exp.cond(mapExists, seatLogic, createNewSeatInNewMap);
        
        WritePolicy wp = client.copyWritePolicyDefault();
        wp.txn = txn;
        client.operate(wp, key, 
                ExpOperation.write("seats", Exp.build(checkSeatExists), ExpWriteFlags.DEFAULT)
            );
    }

    /**
     * Save a venue. The key here will be the same as the name so we will not use sendKey = true
     * @param wp - Write policy to use. Can be null
     * @param venue - Venue to save. Should not be null
     * @param txn - Txn id. Can be null. If passed, then txn on the write policy will be set to this value
     */
    public void save(WritePolicy wp, Venue venue, Txn txn) {
        if (wp == null && txn != null) {
            wp = client.copyWritePolicyDefault();
            wp.txn = txn;
        }
        Key key = new Key(NAMESPACE, VENUE_SET, venue.getId());
        client.put(wp, key, 
                new Bin("address", venue.getAddress()),
                new Bin("city", venue.getCity()),
                new Bin("country", venue.getCountry()),
                new Bin("desc", venue.getDescription()),
                new Bin("name", venue.getName()),
                new Bin("postCode", venue.getPostalCode()),
                new Bin("seatCount", venue.getSeatCount()),
                new Bin("numRows", venue.getNumRows())
            );
    }
    
    public Venue readVenue(String id) {
        Record thisRecord = client.get(null, new Key(NAMESPACE, VENUE_SET, id));
        if (thisRecord == null) {
            return null;
        }
        else {
            Venue result = new Venue();
            result.setAddress(thisRecord.getString("address"));
            result.setCity(thisRecord.getString("city"));
            result.setCountry(thisRecord.getString("country"));
            result.setDescription(thisRecord.getString("desc"));
            result.setName(thisRecord.getString("name"));
            result.setId(id);
            result.setPostalCode(thisRecord.getString("postCode"));
            result.setSeatCount(thisRecord.getInt("seatCount"));
            result.setNumRows(thisRecord.getInt("numRows"));
            return result;
        }
    }
    
    /**
     * Save a booking. 
     * @param wp
     * @param shoppingCart
     * @param txn
     */
    public void save(WritePolicy wp, ShoppingCart shoppingCart, Txn txn) {
        if (wp == null && txn != null) {
            wp = client.copyWritePolicyDefault();
            wp.txn = txn;
        }
        Key key = new Key(NAMESPACE, SHOPPING_CART_SET, shoppingCart.getId());
        List<String> seats = new ArrayList<>(shoppingCart.getSeats().size());
        for (Seat thisSeat: shoppingCart.getSeats()) {
            seats.add(thisSeat.getRow() + "-" + thisSeat.getSeatNumber());
        }
        client.put(wp, key, 
                new Bin("created", toAerospike(shoppingCart.getCreated())),
                new Bin("custId", shoppingCart.getCustId()),
                new Bin("eventId", shoppingCart.getEventId()),
                new Bin("status", shoppingCart.getStatus().toString()),
                new Bin("seats", seats)
            );
    }
    
    public ShoppingCart loadBooking(Policy policy, String shoppingCartId) {
        Record thisRecord = client.get(policy, new Key(NAMESPACE, SHOPPING_CART_SET, shoppingCartId));
        if (thisRecord == null) {
            return null;
        }
        else {
            ShoppingCart result = new ShoppingCart();
            result.setCreated(fromAerospike(thisRecord.getLong("created")));
            result.setCustId(thisRecord.getLong("custId"));
            result.setEventId(thisRecord.getString("eventId"));
            result.setStatus(Status.valueOf(thisRecord.getString("status")));
            List<String> seatList = (List<String>) thisRecord.getList("seats");
            Set<Seat> seats = new HashSet<>();
            for (String thisSeatStr : seatList) {
                String[] parts = thisSeatStr.split("-");
                Seat seat = new Seat(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                seats.add(seat);
            }
            result.setSeats(seats);
            return result;
        }
    }
    
    public void commitTxn(Txn txn) {
        this.client.commit(txn);
    }
    
    public void rollbackTxn(Txn txn) {
        this.client.abort(txn);
    }
}
